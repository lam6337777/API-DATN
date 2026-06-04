package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String name;
    private String slug;
    private String description;
    private String categoryName;
    private String categoryId;
    private String brandName;
    private Long brandId;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private Boolean isActive;
    private Boolean isFeatured;
    private Integer totalSold;
    private Float avgRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private List<ImageDto> images;
    private List<VariantDto> variants;

    @Data
    @Builder
    public static class ImageDto {
        private Long id;
        private String imageUrl;
        private Integer sortOrder;
        private Boolean isPrimary;
    }

    @Data
    @Builder
    public static class VariantDto {
        private Long id;
        private String size;
        private String color;
        private Integer stockQuantity;
        private String sku;
        private Boolean canDelete;
    }
}
