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
     * Đặt hàng từ giỏ hàng
     */
    @Transactional
    public OrderResponse createOrder(Long userId, OrderRequest request, HttpServletRequest httpRequest) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", userId));

        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản của bạn đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.");
        }

        // Lấy giỏ hàng
        List<CartItemEntity> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        // Tạo mã đơn hàng
        String orderCode = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Tính subtotal
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemEntity item : cartItems) {
            ProductVariantEntity variant = item.getVariant();
            ProductEntity product = variant.getProduct();

            // Kiểm tra sản phẩm đã bị xóa / không còn hoạt động
            if (!product.getIsActive()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' đã bị xóa hoặc không còn hoạt động.");
            }

            // Kiểm tra tồn kho
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new BadRequestException("Sản phẩm '" + product.getName() + "' (" + variant.getSize() + "/"
                        + variant.getColor() + ") không đủ tồn kho");
            }

            BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : product.getOriginalPrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Phí shipping cố định
        BigDecimal shippingFee = BigDecimal.ZERO;

        BigDecimal totalAmount = subtotal.add(shippingFee);

        // Tạo đơn hàng
        OrderEntity order = OrderEntity.builder()
                .orderCode(orderCode)
                .user(user)
                .paymentMethod(request.getPaymentMethod())
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .shippingName(request.getShippingName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .build();

        order = orderRepository.save(order);

        // Tạo order items và trừ tồn kho
        for (CartItemEntity cartItem : cartItems) {
            ProductVariantEntity variant = cartItem.getVariant();
            ProductEntity product = variant.getProduct();
            BigDecimal price = product.getSalePrice() != null ? product.getSalePrice() : product.getOriginalPrice();

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

            // Trừ tồn kho
            variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
            productVariantRepository.save(variant);

            // Cộng totalSold
            product.setTotalSold(product.getTotalSold() + cartItem.getQuantity());
        }

        order = orderRepository.save(order);

        // Xóa giỏ hàng
        cartItemRepository.deleteByUserId(userId);

        // Tạo payment record cho COD
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            PaymentEntity payment = PaymentEntity.builder()
                    .order(order)
                    .method(PaymentMethod.COD)
                    .status(PaymentStatus.PENDING)
                    .amount(totalAmount)
                    .build();
            paymentRepository.save(payment);
        }

        // Build response
        OrderResponse response = toOrderResponse(order);



        notificationService.sendOrderNotification(response);
        return response;
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
     * Chi tiết đơn hàng
     */
    public OrderResponse getOrderByCode(String orderCode) {
        OrderEntity order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "mã", orderCode));
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

        order.setStatus(OrderStatus.CANCELLED);

        // Hoàn lại tồn kho
        for (OrderItemEntity item : order.getOrderItems()) {
            ProductVariantEntity variant = item.getVariant();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
            productVariantRepository.save(variant);
        }

        // Nếu đã thanh toán, đánh dấu refund
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

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

        order.setStatus(newStatus);

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

        // Nếu admin huỷ đơn hàng, hoàn lại tồn kho
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItemEntity item : order.getOrderItems()) {
                ProductVariantEntity variant = item.getVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }

            // Nếu đã thanh toán, đánh dấu refund
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }

        orderRepository.save(order);
        return toOrderResponse(order);
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
                            .productId(item.getVariant().getProduct().getId())
                            .isReviewed(reviewedProductIds.contains(item.getVariant().getProduct().getId()))
                            .build();
                })
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .shippingName(order.getShippingName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .user(authService.toUserResponse(order.getUser(), null))
                .items(items)
                .build();
    }
}
