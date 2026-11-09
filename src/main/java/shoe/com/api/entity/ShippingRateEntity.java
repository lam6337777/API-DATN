package shoe.com.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Phí ship theo tỉnh/thành. Dòng có province = "DEFAULT" áp dụng cho các tỉnh không có cấu hình riêng.
 */
@Entity
@Table(name = "shipping_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingRateEntity {

    public static final String DEFAULT_PROVINCE = "DEFAULT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String province;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal fee;

    /** Ví dụ: "1-2 ngày" */
    @Column(name = "estimated_days")
    private String estimatedDays;
}
