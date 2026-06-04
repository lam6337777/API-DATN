package shoe.com.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import shoe.com.api.enums.ReturnRequestType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestRequest {
    @NotBlank(message = "Mã đơn hàng không được để trống")
    private String orderCode;

    @NotNull(message = "Loại yêu cầu không được để trống")
    private ReturnRequestType type;

    @NotBlank(message = "Lý do không được để trống")
    private String reason;

    @NotEmpty(message = "Danh sách mặt hàng không được để trống")
    private List<Long> orderItemIds;

    private Long exchangeVariantId; // Chỉ dùng cho EXCHANGE
}
