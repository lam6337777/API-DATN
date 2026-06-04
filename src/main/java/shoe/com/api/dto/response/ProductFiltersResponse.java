package shoe.com.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFiltersResponse {
    private List<String> categories;
    private List<String> brands;
    private java.math.BigDecimal maxPrice;
}
