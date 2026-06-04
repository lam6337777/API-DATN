package shoe.com.api.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.BrandEntity;
import shoe.com.api.service.BrandService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandEntity>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success(brandService.getAllBrands()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandEntity>> createBrand(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String logoUrl) {
        BrandEntity brand = brandService.createBrand(name, description, logoUrl);
        return ResponseEntity.ok(ApiResponse.success("Tạo thương hiệu thành công", brand));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandEntity>> updateBrand(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String logoUrl) {
        BrandEntity brand = brandService.updateBrand(id, name, description, logoUrl);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công", brand));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa thương hiệu", null));
    }
}
