package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shoe.com.api.dto.request.VoucherRequest;
import shoe.com.api.dto.response.VoucherCheckResponse;
import shoe.com.api.entity.VoucherEntity;
import shoe.com.api.enums.DiscountType;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.OrderRepository;
import shoe.com.api.repository.VoucherRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final OrderRepository orderRepository;

    // ===================== Khách hàng =====================

    public List<VoucherEntity> getAvailableVouchers() {
        return voucherRepository.findAvailable(LocalDateTime.now());
    }

    /** Kiểm tra mã và tính số tiền được giảm cho một giá trị giỏ hàng. */
    public VoucherCheckResponse check(String code, BigDecimal subtotal, Long userId) {
        VoucherEntity voucher = getUsableVoucher(code, userId);
        BigDecimal discount = calculateDiscount(voucher, subtotal);
        return VoucherCheckResponse.builder()
                .code(voucher.getCode())
                .description(voucher.getDescription())
                .discountAmount(discount)
                .build();
    }

    /**
     * Kiểm tra mã còn dùng được với khách này không (đang bật, trong thời hạn, còn lượt).
     * Ném BadRequestException với thông báo tiếng Việt nếu không hợp lệ.
     */
    public VoucherEntity getUsableVoucher(String code, Long userId) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Vui lòng nhập mã giảm giá");
        }
        VoucherEntity voucher = voucherRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại"));

        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new BadRequestException("Mã giảm giá đã bị vô hiệu hóa");
        }
        if (voucher.getStartAt() != null && now.isBefore(voucher.getStartAt())) {
            throw new BadRequestException("Mã giảm giá chưa đến thời gian sử dụng");
        }
        if (voucher.getEndAt() != null && now.isAfter(voucher.getEndAt())) {
            throw new BadRequestException("Mã giảm giá đã hết hạn");
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (userId != null && voucher.getPerUserLimit() != null) {
            long used = orderRepository.countByUserIdAndVoucherCodeIgnoreCaseAndStatusNot(
                    userId, voucher.getCode(), OrderStatus.CANCELLED);
            if (used >= voucher.getPerUserLimit()) {
                throw new BadRequestException("Bạn đã dùng hết lượt cho mã giảm giá này");
            }
        }
        return voucher;
    }

    public BigDecimal calculateDiscount(VoucherEntity voucher, BigDecimal subtotal) {
        if (subtotal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new BadRequestException(String.format(java.util.Locale.forLanguageTag("vi-VN"),
                    "Đơn hàng tối thiểu %,.0fđ để dùng mã %s", voucher.getMinOrderValue(), voucher.getCode()));
        }

        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = subtotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
                discount = voucher.getMaxDiscount();
            }
        } else {
            discount = voucher.getDiscountValue();
        }
        // Không giảm quá giá trị đơn
        return discount.min(subtotal);
    }

    /** Tăng lượt dùng khi đặt hàng. Dùng câu UPDATE có điều kiện nên an toàn khi nhiều người đặt cùng lúc. */
    @Transactional
    public void redeem(VoucherEntity voucher) {
        if (voucherRepository.incrementUsage(voucher.getId()) == 0) {
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        }
    }

    /** Hoàn lại lượt dùng khi đơn bị hủy. */
    @Transactional
    public void release(String code) {
        if (code != null && !code.isBlank()) {
            voucherRepository.decrementUsage(code);
        }
    }

    // ===================== Admin =====================

    public List<VoucherEntity> getAll() {
        return voucherRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public VoucherEntity create(VoucherRequest request) {
        if (voucherRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Mã giảm giá đã tồn tại");
        }
        VoucherEntity voucher = new VoucherEntity();
        voucher.setUsedCount(0);
        apply(voucher, request);
        return voucherRepository.save(voucher);
    }

    @Transactional
    public VoucherEntity update(Long id, VoucherRequest request) {
        VoucherEntity voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá", "id", id));
        if (!voucher.getCode().equalsIgnoreCase(request.getCode())
                && voucherRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new BadRequestException("Mã giảm giá đã tồn tại");
        }
        apply(voucher, request);
        return voucherRepository.save(voucher);
    }

    @Transactional
    public void delete(Long id) {
        VoucherEntity voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá", "id", id));
        if (voucher.getUsedCount() > 0) {
            // Đã có đơn dùng mã: chỉ tắt để giữ lịch sử
            voucher.setIsActive(false);
            voucherRepository.save(voucher);
        } else {
            voucherRepository.delete(voucher);
        }
    }

    private void apply(VoucherEntity voucher, VoucherRequest request) {
        if (request.getDiscountType() == DiscountType.PERCENT
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Phần trăm giảm không được vượt quá 100");
        }
        if (request.getStartAt() != null && request.getEndAt() != null
                && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        voucher.setCode(request.getCode().trim().toUpperCase());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscount(request.getDiscountType() == DiscountType.PERCENT ? request.getMaxDiscount() : null);
        voucher.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        voucher.setStartAt(request.getStartAt());
        voucher.setEndAt(request.getEndAt());
        voucher.setUsageLimit(request.getUsageLimit());
        voucher.setPerUserLimit(request.getPerUserLimit());
        voucher.setIsActive(request.getIsActive() == null || request.getIsActive());
    }
}
