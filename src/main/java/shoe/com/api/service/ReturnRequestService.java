package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.service.NotificationService;
import shoe.com.api.dto.request.ReturnRequestRequest;
import shoe.com.api.dto.response.ProductResponse;
import shoe.com.api.dto.response.ReturnRequestResponse;
import shoe.com.api.entity.*;
import shoe.com.api.enums.OrderItemStatus;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.enums.PaymentStatus;
import shoe.com.api.enums.ReturnRequestStatus;
import shoe.com.api.enums.ReturnRequestType;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ReturnRequestResponse createRequest(Long userId, ReturnRequestRequest request) {
        OrderEntity order = orderRepository.findByOrderCode(request.getOrderCode())
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "mã", request.getOrderCode()));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền yêu cầu cho đơn hàng này");
        }

        if (!order.getUser().getIsActive()) {
            throw new BadRequestException("Tài khoản của bạn đã bị vô hiệu hóa.");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể yêu cầu đổi/trả cho đơn hàng đã giao thành công");
        }

        // Kiểm tra thời hạn
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deliveredAt = order.getDeliveredAt() != null ? order.getDeliveredAt() : order.getCreatedAt();
        long daysSinceDelivered = ChronoUnit.DAYS.between(deliveredAt, now);

        if (request.getType() == ReturnRequestType.RETURN || request.getType() == ReturnRequestType.EXCHANGE) {
            if (daysSinceDelivered > 7) {
                throw new BadRequestException("Đã quá thời hạn 7 ngày để đổi/trả hàng");
            }
        } else if (request.getType() == ReturnRequestType.WARRANTY) {
            if (daysSinceDelivered > 30) {
                throw new BadRequestException("Đã quá thời hạn 30 ngày để yêu cầu bảo hành");
            }
        }

        List<OrderItemEntity> items = orderItemRepository.findAllById(request.getOrderItemIds());
        if (items.isEmpty()) {
            throw new BadRequestException("Danh sách mặt hàng không hợp lệ");
        }

        // Kiểm tra xem các mặt hàng có thuộc đơn hàng này không và trạng thái của chúng
        for (OrderItemEntity item : items) {
            if (!item.getOrder().getId().equals(order.getId())) {
                throw new BadRequestException("Mặt hàng " + item.getProductName() + " không thuộc đơn hàng này");
            }
            if (item.getStatus() != null && item.getStatus() != OrderItemStatus.ACTIVE) {
                throw new BadRequestException(
                        "Mặt hàng " + item.getProductName() + " đang trong một yêu cầu khác hoặc đã được đổi/trả");
            }
        }

        ProductVariantEntity exchangeVariant = null;
        if (request.getType() == ReturnRequestType.EXCHANGE && request.getExchangeVariantId() != null) {
            exchangeVariant = productVariantRepository.findById(request.getExchangeVariantId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Sản phẩm đổi", "id", request.getExchangeVariantId()));
        }

        ReturnRequestEntity returnRequest = ReturnRequestEntity.builder()
                .order(order)
                .user(order.getUser())
                .type(request.getType())
                .reason(request.getReason())
                .status(ReturnRequestStatus.PENDING)
                .returnItems(items)
                .exchangeVariant(exchangeVariant)
                .build();

        // Cập nhật trạng thái item sang REQUESTED để tránh yêu cầu trùng lặp
        OrderItemStatus requestedStatus = switch (request.getType()) {
            case RETURN -> OrderItemStatus.RETURN_REQUESTED;
            case EXCHANGE -> OrderItemStatus.EXCHANGE_REQUESTED;
            case WARRANTY -> OrderItemStatus.WARRANTY_REQUESTED;
        };

        items.forEach(item -> item.setStatus(requestedStatus));
        orderItemRepository.saveAll(items);

        // Cập nhật trạng thái đơn hàng
        order.setStatus(switch (request.getType()) {
            case RETURN -> OrderStatus.RETURN_REQUESTED;
            case EXCHANGE -> OrderStatus.EXCHANGE_REQUESTED;
            case WARRANTY -> OrderStatus.WARRANTY_REQUESTED;
        });
        orderRepository.save(order);

        returnRequest = returnRequestRepository.save(returnRequest);
        ReturnRequestResponse response = toResponse(returnRequest);
        notificationService.sendReturnNotification(response);
        return response;
    }

    public List<ReturnRequestResponse> getAllRequests() {
        return returnRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ReturnRequestResponse getRequestById(Long id) {
        ReturnRequestEntity request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đổi/trả", "id", id));
        return toResponse(request);
    }

    @Transactional
    public ReturnRequestResponse approveRequest(Long requestId, String adminNote) {
        ReturnRequestEntity request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đổi/trả", "id", requestId));

        if (request.getStatus() != ReturnRequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        request.setStatus(ReturnRequestStatus.APPROVED);
        request.setAdminNote(adminNote);

        OrderItemStatus approvedStatus = switch (request.getType()) {
            case RETURN -> OrderItemStatus.RETURNED;
            case EXCHANGE -> OrderItemStatus.EXCHANGED;
            case WARRANTY -> OrderItemStatus.WARRANTY_COMPLETED;
        };

        // Cập nhật trạng thái item
        for (OrderItemEntity item : request.getReturnItems()) {
            item.setStatus(approvedStatus);

            // Cập nhật tồn kho nếu là Hoàn hàng hoặc Đổi hàng
            if (request.getType() == ReturnRequestType.RETURN || request.getType() == ReturnRequestType.EXCHANGE) {
                ProductVariantEntity variant = item.getVariant();
                variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
                productVariantRepository.save(variant);
            }
        }

        // Nếu là Đổi hàng, trừ kho sản phẩm mới
        if (request.getType() == ReturnRequestType.EXCHANGE && request.getExchangeVariant() != null) {
            ProductVariantEntity exchangeVariant = request.getExchangeVariant();
            // Ở đây có thể thêm kiểm tra tồn kho nếu cần
            exchangeVariant.setStockQuantity(exchangeVariant.getStockQuantity() - 1); // Giả định đổi 1
            productVariantRepository.save(exchangeVariant);
        }

        // Cập nhật trạng thái đơn hàng
        OrderEntity order = request.getOrder();
        order.setStatus(switch (request.getType()) {
            case RETURN -> OrderStatus.RETURNED;
            case EXCHANGE -> OrderStatus.EXCHANGED;
            case WARRANTY -> OrderStatus.WARRANTY_COMPLETED;
        });

        if (order.getStatus() == OrderStatus.RETURNED && order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        orderRepository.save(order);

        orderItemRepository.saveAll(request.getReturnItems());
        returnRequestRepository.save(request);

        return toResponse(request);
    }

    @Transactional
    public ReturnRequestResponse rejectRequest(Long requestId, String adminNote) {
        ReturnRequestEntity request = returnRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đổi/trả", "id", requestId));

        if (request.getStatus() != ReturnRequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        request.setStatus(ReturnRequestStatus.REJECTED);
        request.setAdminNote(adminNote);

        // Khôi phục trạng thái ACTIVE cho các mặt hàng
        for (OrderItemEntity item : request.getReturnItems()) {
            item.setStatus(OrderItemStatus.ACTIVE);
        }

        // Khôi phục trạng thái đơn hàng về DELIVERED
        OrderEntity order = request.getOrder();
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        orderItemRepository.saveAll(request.getReturnItems());
        returnRequestRepository.save(request);

        return toResponse(request);
    }

    public ReturnRequestResponse toResponse(ReturnRequestEntity entity) {
        return ReturnRequestResponse.builder()
                .id(entity.getId())
                .orderCode(entity.getOrder().getOrderCode())
                .type(entity.getType())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .adminNote(entity.getAdminNote())
                .createdAt(entity.getCreatedAt())
                .exchangeVariant(entity.getExchangeVariant() != null ? ProductResponse.VariantDto.builder()
                        .id(entity.getExchangeVariant().getId())
                        .size(entity.getExchangeVariant().getSize())
                        .color(entity.getExchangeVariant().getColor())
                        .stockQuantity(entity.getExchangeVariant().getStockQuantity())
                        .sku(entity.getExchangeVariant().getSku())
                        .build() : null)
                .user(ReturnRequestResponse.UserDto.builder()
                        .id(entity.getUser().getId())
                        .fullName(entity.getUser().getFullName())
                        .email(entity.getUser().getEmail())
                        .build())
                .returnItems(entity.getReturnItems().stream()
                        .map(item -> ReturnRequestResponse.OrderItemResponse.builder()
                                .id(item.getId())
                                .productName(item.getProductName())
                                .size(item.getSize())
                                .color(item.getColor())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
