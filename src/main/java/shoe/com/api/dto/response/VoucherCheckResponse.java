package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VoucherCheckResponse {
    private String code;
    private String description;
    private BigDecimal discountAmount;
}
