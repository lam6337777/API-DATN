package shoe.com.api.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.request.OrderRequest;
import shoe.com.api.dto.response.OrderResponse;
import shoe.com.api.entity.*;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.enums.PaymentMethod;
import shoe.com.api.enums.PaymentStatus;
import shoe.com.api.enums.Role;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;

    private final AuthService authService;
    private final NotificationService notificationService;
    private final VoucherService voucherService;
    private final ShippingService shippingService;

    /**
     * Admin: Lấy danh sách đơn hàng (Page DTO) với bộ lọc
     */
    public Page<OrderResponse> getAllOrders(OrderStatus status, String search, LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Tiền xử lý search để tránh lỗi Postgres type inference với CONCAT/LOWER
        String searchParam = (search != null && !search.trim().isEmpty())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        Page<OrderEntity> orders = orderRepository.findAllWithFilters(status, searchParam, startDate, endDate, pageable);
        return orders.map(this::toOrderResponse);
    }

    /**
     * Đặt hàng từ giỏ hàng.
     * Phí ship và giảm giá luôn được tính lại ở backend, không tin số tiền từ client.
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request, HttpServletRequest httpRequest) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        if (request.getPaymentMethod() != PaymentMethod.COD) {
            throw new BadRequestException("Phương thức thanh toán này hiện chưa được hỗ trợ");
        }

        // Lấy giỏ hàng
        List<CartItemEntity> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        // Tạo mã đơn hàng
        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Tính subtotal và kiểm tra sản phẩm
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemEntity item : cartItems) {
            ProductEntity product = item.getVariant().getProduct();
            if (!product.getIsActive()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' đã bị xóa hoặc không còn hoạt động.");
            }
            subtotal = subtotal.add(effectivePrice(product).multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Phí ship theo tỉnh/thành
        BigDecimal shippingFee = shippingService.quote(request.getProvince(), subtotal).getFee();

        // Mã giảm giá
        VoucherEntity voucher = null;
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            voucher = voucherService.getUsableVoucher(request.getVoucherCode(), userId);
            discount = voucherService.calculateDiscount(voucher, subtotal);
            voucherService.redeem(voucher);
        }

        BigDecimal totalAmount = subtotal.subtract(discount).add(shippingFee).max(BigDecimal.ZERO);

        // Tạo đơn hàng
        OrderEntity order = OrderEntity.builder()
                .orderCode(orderCode)
                .user(user)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discount)
                .voucherCode(voucher != null ? voucher.getCode() : null)
                .totalAmount(totalAmount)
                .shippingName(request.getShippingName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .build();
        order.changeStatus(OrderStatus.PENDING, "Đặt hàng thành công");

        // Tạo order items và trừ tồn kho
        for (CartItemEntity cartItem : cartItems) {
            ProductVariantEntity variant = cartItem.getVariant();
            ProductEntity product = variant.getProduct();
            BigDecimal price = effectivePrice(product);

            // Trừ kho nguyên tử: nếu người khác vừa mua hết thì câu UPDATE trả về 0
            int updated = productVariantRepository.decrementStock(variant.getId(), cartItem.getQuantity());
            if (updated == 0) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' (size " + variant.getSize()
                        + ") không đủ tồn kho");
            }

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .variant(variant)
                    .productName(product.getName())
                    .size(variant.getSize())
                    .color(variant.getColor())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(price)
                    .totalPrice(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .build();
            order.getOrderItems().add(orderItem);

            product.setTotalSold(product.getTotalSold() + cartItem.getQuantity());
        }

        order = orderRepository.save(order);

        // Xóa giỏ hàng
        cartItemRepository.deleteByUserId(userId);

        // Tạo payment record cho COD
        PaymentEntity payment = PaymentEntity.builder()
                .order(order)
                .method(PaymentMethod.COD)
                .status(PaymentStatus.PENDING)
                .amount(totalAmount)
                .build();
        paymentRepository.save(payment);

        OrderResponse response = toOrderResponse(order);
        notificationService.sendOrderNotification(response);
        return response;
    }

    private static BigDecimal effectivePrice(ProductEntity product) {
        return product.getSalePrice() != null ? product.getSalePrice() : product.getOriginalPrice();
    }

    /**
     * Lịch sử đơn hàng
     */
    public Page<OrderResponse> getUserOrders(Long userId, OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderEntity> orders;

        if (status != null) {
            orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        } else {
            orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return orders.map(this::toOrderResponse);
    }

    /**
     * Chi tiết đơn hàng cho Admin (không kiểm tra chủ sở hữu)
     */
    public OrderResponse getOrderByCode(String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "mã", orderCode));
        return toOrderResponse(order);
    }

    /**
     * Chi tiết đơn hàng cho khách: chỉ chủ đơn (hoặc Admin) mới xem được
     */
    public OrderResponse getOrderForUser(UserEntity user, String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "mã", orderCode));
        boolean isOwner = order.getUser().getId().equals(user.getId());
        if (!isOwner && user.getRole() != Role.ADMIN) {
            // Trả về "không tìm thấy" để không lộ việc mã đơn có tồn tại
            throw new ResourceNotFoundException("Đơn hàng", "mã", orderCode);
        }
        return toOrderResponse(order);
    }

    /**
     * Hủy đơn hàng
     */
    @Transactional
    public OrderResponse cancelOrder(Long userId, String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "mã", orderCode));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Không có quyền hủy đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng ở trạng thái 'Chờ xác nhận'");
        }

        order.changeStatus(OrderStatus.CANCELLED, "Khách hàng hủy đơn");
        restoreStockAndVoucher(order);

        orderRepository.save(order);
        return toOrderResponse(order);
    }

    /**
     * Admin: Cập nhật trạng thái đơn hàng
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        // Validate trạng thái hợp lệ
        validateStatusTransition(order.getStatus(), newStatus);

        order.changeStatus(newStatus, statusNote(newStatus));

        // Nếu giao thành công + COD → cập nhật payment = PAID
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
            // Cập nhật payment record
            List<PaymentEntity> payments = paymentRepository.findByOrderId(orderId);
            for (PaymentEntity payment : payments) {
                if (payment.getMethod() == PaymentMethod.COD && payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            }
        }

        // Nếu admin huỷ đơn hàng, hoàn lại tồn kho và lượt dùng mã
        if (newStatus == OrderStatus.CANCELLED) {
            restoreStockAndVoucher(order);
        }

        orderRepository.save(order);
        return toOrderResponse(order);
    }

    private void restoreStockAndVoucher(OrderEntity order) {
        for (OrderItemEntity item : order.getOrderItems()) {
            ProductVariantEntity variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }
        voucherService.release(order.getVoucherCode());

        // Nếu đã thanh toán, đánh dấu refund
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
    }

    private static String statusNote(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> "Shop đã xác nhận đơn hàng";
            case SHIPPING -> "Đơn hàng đã được giao cho đơn vị vận chuyển";
            case DELIVERED -> "Giao hàng thành công";
            case CANCELLED -> "Shop đã hủy đơn hàng";
            case RETURNED -> "Đã hoàn trả hàng";
            default -> null;
        };
    }

    // Validate chuyển trạng thái hợp lệ
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.CONFIRMED || next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case DELIVERED -> next == OrderStatus.RETURN_REQUESTED ||
                             next == OrderStatus.EXCHANGE_REQUESTED ||
                             next == OrderStatus.WARRANTY_REQUESTED ||
                             next == OrderStatus.RETURNED;
            case CANCELLED -> false;
            default -> false;
        };

        if (!valid) {
            throw new BadRequestException("Không thể chuyển trạng thái từ " + current + " sang " + next);
        }
    }

    // Chuyển Entity sang DTO
    public OrderResponse toOrderResponse(OrderEntity order) {
        List<ReviewEntity> orderReviews = reviewRepository.findByOrderId(order.getId());
        Set<String> reviewedProductIds = orderReviews.stream()
                .map(r -> r.getProduct().getId())
                .collect(Collectors.toSet());

        List<OrderResponse.OrderItemDto> items = order.getOrderItems().stream()
                .map(item -> {
                    String imageUrl = null;
                    ProductEntity product = item.getVariant().getProduct();
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        imageUrl = product.getImages().stream()
                                .filter(ProductImageEntity::getIsPrimary)
                                .findFirst()
                                .map(ProductImageEntity::getImageUrl)
                                .orElse(product.getImages().get(0).getImageUrl());
                    }

                    return OrderResponse.OrderItemDto.builder()
                            .id(item.getId())
                            .productName(item.getProductName())
                            .size(item.getSize())
                            .color(item.getColor())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .totalPrice(item.getTotalPrice())
                            .status(item.getStatus())
                            .imageUrl(imageUrl)
                            .productId(product.getId())
                            .isReviewed(reviewedProductIds.contains(product.getId()))
                            .build();
                })
                .collect(Collectors.toList());

        List<OrderResponse.StatusHistoryDto> history = order.getStatusHistory().stream()
                .map(h -> OrderResponse.StatusHistoryDto.builder()
                        .status(h.getStatus())
                        .note(h.getNote())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .voucherCode(order.getVoucherCode())
                .totalAmount(order.getTotalAmount())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .deliveredAt(order.getDeliveredAt())
                .user(authService.toUserResponse(order.getUser(), null))
                .items(items)
                .statusHistory(history)
                .build();
    }
}
