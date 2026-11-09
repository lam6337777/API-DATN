package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.request.ShippingRateRequest;
import shoe.com.api.dto.response.ShippingQuoteResponse;
import shoe.com.api.entity.ShippingRateEntity;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.ShippingRateRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private final ShippingRateRepository shippingRateRepository;

    /** Đơn từ mức này trở lên được miễn phí ship. 0 = tắt. */
    @Value("${app.shipping.free-threshold:1000000}")
    private BigDecimal freeShippingThreshold;

    /** Phí dùng khi chưa cấu hình dòng DEFAULT trong bảng shipping_rates. */
    @Value("${app.shipping.default-fee:35000}")
    private BigDecimal fallbackFee;

    public ShippingQuoteResponse quote(String province, BigDecimal subtotal) {
        ShippingRateEntity rate = findRate(province);
        BigDecimal fee = rate != null ? rate.getFee() : fallbackFee;
        String days = rate != null ? rate.getEstimatedDays() : "3-5 ngày";

        boolean free = subtotal != null
                && freeShippingThreshold.signum() > 0
                && subtotal.compareTo(freeShippingThreshold) >= 0;

        return ShippingQuoteResponse.builder()
                .province(province)
                .fee(free ? BigDecimal.ZERO : fee)
                .estimatedDays(days)
                .freeShippingThreshold(freeShippingThreshold)
                .freeShipping(free)
                .build();
    }

    private ShippingRateEntity findRate(String province) {
        if (province != null && !province.isBlank()) {
            var exact = shippingRateRepository.findByProvinceIgnoreCase(province.trim());
            if (exact.isPresent()) {
                return exact.get();
            }
        }
        return shippingRateRepository.findByProvinceIgnoreCase(ShippingRateEntity.DEFAULT_PROVINCE).orElse(null);
    }

    // ===================== Admin =====================

    public List<ShippingRateEntity> getAll() {
        return shippingRateRepository.findAllByOrderByProvinceAsc();
    }

    @Transactional
    public ShippingRateEntity upsert(ShippingRateRequest request) {
        ShippingRateEntity rate = shippingRateRepository.findByProvinceIgnoreCase(request.getProvince().trim())
                .orElseGet(ShippingRateEntity::new);
        rate.setProvince(request.getProvince().trim());
        rate.setFee(request.getFee());
        rate.setEstimatedDays(request.getEstimatedDays());
        return shippingRateRepository.save(rate);
    }

    @Transactional
    public void delete(Long id) {
        ShippingRateEntity rate = shippingRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phí vận chuyển", "id", id));
        if (ShippingRateEntity.DEFAULT_PROVINCE.equalsIgnoreCase(rate.getProvince())) {
            throw new BadRequestException("Không thể xóa mức phí mặc định");
        }
        shippingRateRepository.delete(rate);
    }
}
