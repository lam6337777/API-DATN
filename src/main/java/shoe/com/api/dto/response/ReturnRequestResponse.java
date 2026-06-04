package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;
import shoe.com.api.enums.ReturnRequestStatus;
import shoe.com.api.enums.ReturnRequestType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReturnRequestResponse {
    private Long id;
    private String orderCode;
    private ReturnRequestType type;
    private String reason;
    private ReturnRequestStatus status;
    private String adminNote;
    private List<OrderItemResponse> returnItems;
    private ProductResponse.VariantDto exchangeVariant;
    private LocalDateTime createdAt;
    private UserDto user;

    @Data
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private String productName;
        private String size;
        private String color;
        private Integer quantity;
    }

    @Data
    @Builder
    public static class UserDto {
        private Long id;
        private String fullName;
        private String email;
    }
}
