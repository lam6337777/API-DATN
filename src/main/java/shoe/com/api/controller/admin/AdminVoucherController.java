package shoe.com.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.VoucherRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.VoucherEntity;
import shoe.com.api.service.VoucherService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VoucherEntity>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(voucherService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VoucherEntity>> create(@Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã tạo mã giảm giá", voucherService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VoucherEntity>> update(@PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật mã giảm giá", voucherService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        voucherService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa mã giảm giá", null));
    }
}
