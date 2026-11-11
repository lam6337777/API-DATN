package shoe.com.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private String fullName;
    private String avatarUrl;
    private String productId;
    private String productName;
    private String orderCode;
    private Integer rating;
    private String comment;
    private String productImage;
    /** Phân loại khách đã mua (size/màu) — hiển thị "Phân loại: Size 42" */
    private String variantSize;
    private String variantColor;
    private LocalDateTime createdAt;
}
