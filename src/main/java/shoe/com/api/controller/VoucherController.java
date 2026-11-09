package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.VoucherCheckResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.entity.VoucherEntity;
import shoe.com.api.service.VoucherService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    // Danh sách mã đang khả dụng (công khai, hiển thị ở trang chủ/checkout)
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<VoucherEntity>>> getAvailable() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getAvailableVouchers()));
    }

    // Kiểm tra mã và tính số tiền giảm cho giỏ hàng hiện tại
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<VoucherCheckResponse>> check(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal,
            @AuthenticationPrincipal UserEntity user) {
        VoucherCheckResponse result = voucherService.check(code, subtotal, user != null ? user.getId() : null);
        return ResponseEntity.ok(ApiResponse.success("Áp dụng mã thành công", result));
    }
}
