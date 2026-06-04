package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.BrandEntity;
import shoe.com.api.service.BrandService;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandEntity>>> getBrands() {
        List<BrandEntity> brands = brandService.getAllActiveBrands();
        return ResponseEntity.ok(ApiResponse.success(brands));
    }
}
