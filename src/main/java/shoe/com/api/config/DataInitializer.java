package shoe.com.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import shoe.com.api.entity.ShippingRateEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.entity.VoucherEntity;
import shoe.com.api.enums.DiscountType;
import shoe.com.api.enums.Role;
import shoe.com.api.repository.ShippingRateRepository;
import shoe.com.api.repository.UserRepository;
import shoe.com.api.repository.VoucherRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShippingRateRepository shippingRateRepository;
    private final VoucherRepository voucherRepository;

    @Override
    public void run(String... args) {
        // Tạo tài khoản Admin nếu chưa tồn tại
        if (userRepository.findByEmail("admin@pf2.com").isEmpty()) {
            UserEntity admin = UserEntity.builder()
                    .email("admin@pf2.com")
                    .password(passwordEncoder.encode("adminpf2"))
                    .fullName("Admin PF2")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Tài khoản Admin đã được tạo: admin@pf2.com / adminpf2");
        } else {
            System.out.println("ℹ️ Tài khoản Admin admin@pf2.com đã tồn tại.");
        }

        seedShippingRates();
        seedVouchers();
    }

    // Phí ship mẫu (Admin có thể sửa trong trang quản trị)
    private void seedShippingRates() {
        if (shippingRateRepository.count() > 0) {
            return;
        }
        shippingRateRepository.saveAll(List.of(
                rate(ShippingRateEntity.DEFAULT_PROVINCE, 35000, "3-5 ngày"),
                rate("Hồ Chí Minh", 20000, "1-2 ngày"),
                rate("Hà Nội", 30000, "2-3 ngày"),
                rate("Đà Nẵng", 30000, "2-3 ngày")));
        System.out.println("✅ Đã tạo phí vận chuyển mẫu");
    }

    private static ShippingRateEntity rate(String province, long fee, String days) {
        return ShippingRateEntity.builder().province(province).fee(BigDecimal.valueOf(fee)).estimatedDays(days).build();
    }

    // Mã giảm giá mẫu để demo
    private void seedVouchers() {
        if (voucherRepository.count() > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        voucherRepository.saveAll(List.of(
                VoucherEntity.builder()
                        .code("WELCOME50K").description("Giảm 50.000đ cho đơn từ 500.000đ")
                        .discountType(DiscountType.FIXED).discountValue(BigDecimal.valueOf(50000))
                        .minOrderValue(BigDecimal.valueOf(500000)).perUserLimit(1)
                        .startAt(now).endAt(now.plusMonths(6)).build(),
                VoucherEntity.builder()
                        .code("PF10").description("Giảm 10%, tối đa 100.000đ cho đơn từ 1.000.000đ")
                        .discountType(DiscountType.PERCENT).discountValue(BigDecimal.TEN)
                        .maxDiscount(BigDecimal.valueOf(100000)).minOrderValue(BigDecimal.valueOf(1000000))
                        .usageLimit(500).startAt(now).endAt(now.plusMonths(3)).build(),
                VoucherEntity.builder()
                        .code("PF20").description("Giảm 20%, tối đa 300.000đ cho đơn từ 2.000.000đ")
                        .discountType(DiscountType.PERCENT).discountValue(BigDecimal.valueOf(20))
                        .maxDiscount(BigDecimal.valueOf(300000)).minOrderValue(BigDecimal.valueOf(2000000))
                        .usageLimit(100).perUserLimit(1).startAt(now).endAt(now.plusMonths(1)).build()));
        System.out.println("✅ Đã tạo mã giảm giá mẫu: WELCOME50K, PF10, PF20");
    }
}
