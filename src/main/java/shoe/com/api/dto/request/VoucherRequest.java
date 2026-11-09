package shoe.com.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import shoe.com.api.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {

    @NotBlank(message = "Mã giảm giá không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,30}$", message = "Mã chỉ gồm chữ, số, '-' hoặc '_' (3-30 ký tự)")
    private String code;

    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "1", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    @DecimalMin(value = "0", message = "Giá trị đơn tối thiểu không hợp lệ")
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Min(value = 1, message = "Số lượt dùng phải lớn hơn 0")
    private Integer usageLimit;

    @Min(value = 1, message = "Số lượt mỗi khách phải lớn hơn 0")
    private Integer perUserLimit;

    private Boolean isActive = true;
}
