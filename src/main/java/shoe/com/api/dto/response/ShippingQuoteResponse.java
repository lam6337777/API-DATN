package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShippingQuoteResponse {
    private String province;
    private BigDecimal fee;
    private String estimatedDays;
    /** Ngưỡng miễn phí ship (0 = không áp dụng) */
    private BigDecimal freeShippingThreshold;
    private boolean freeShipping;
}
