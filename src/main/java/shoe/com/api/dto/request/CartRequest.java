package shoe.com.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartRequest {

    @NotNull(message = "Variant ID không được để trống")
    private Long variantId;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    private Integer quantity = 1;
}
