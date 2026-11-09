package shoe.com.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingRateRequest {

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    @NotNull(message = "Phí ship không được để trống")
    @DecimalMin(value = "0", message = "Phí ship không hợp lệ")
    private BigDecimal fee;

    private String estimatedDays;
}
