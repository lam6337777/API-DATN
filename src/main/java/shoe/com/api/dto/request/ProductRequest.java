package shoe.com.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    private String categoryId;

    private Long brandId;

    @NotNull(message = "Giá gốc không được để trống")
    private BigDecimal originalPrice;

    private BigDecimal salePrice;

    private Boolean isFeatured = false;

    private List<VariantRequest> variants;

    private List<String> imageUrls;

    @Data
    public static class VariantRequest {
        private Long id; // null nếu là biến thể mới
        @NotBlank(message = "Size không được để trống")
        private String size;
        private Integer stockQuantity = 0;
        private String sku;
    }
}
