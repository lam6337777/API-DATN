package shoe.com.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.ShippingRateRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.ShippingRateEntity;
import shoe.com.api.service.ShippingService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shipping-rates")
@RequiredArgsConstructor
public class AdminShippingController {

    private final ShippingService shippingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShippingRateEntity>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getAll()));
    }

    // Thêm mới hoặc cập nhật phí theo tỉnh (province = "DEFAULT" là mức mặc định)
    @PutMapping
    public ResponseEntity<ApiResponse<ShippingRateEntity>> upsert(@Valid @RequestBody ShippingRateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu phí vận chuyển", shippingService.upsert(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shippingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa phí vận chuyển", null));
    }
}
