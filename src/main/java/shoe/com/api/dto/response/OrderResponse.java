package shoe.com.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.enums.PaymentMethod;
import shoe.com.api.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String orderCode;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String note;
    private LocalDateTime createdAt;
    private UserResponse user;
    private List<OrderItemDto> items;



    @Data
    @Builder
    public static class OrderItemDto {
        private Long id;
        private String productName;
        private String size;
        private String color;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private String imageUrl;
        private String productId;
        private shoe.com.api.enums.OrderItemStatus status;
        
        @JsonProperty("isReviewed")
        private Boolean isReviewed;
    }
}
