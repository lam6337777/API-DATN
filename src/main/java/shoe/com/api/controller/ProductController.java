package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.ProductFiltersResponse;
import shoe.com.api.dto.response.ProductResponse;
import shoe.com.api.service.ProductService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Lấy danh sách Brand và Category để filter (trả về dạng String)
    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<ProductFiltersResponse>> getProductFilters() {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductFilters()));
    }

    // Danh sách sản phẩm (pagination + filter)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String productSize,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<ProductResponse> products = productService.getProducts(categoryName, brandName, minPrice, maxPrice,
                productSize, q,
                sortBy,
                page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // Chi tiết sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    // Tìm kiếm sản phẩm
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        Page<ProductResponse> products = productService.searchProducts(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    // Sản phẩm nổi bật
    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "8") int limit) {
        List<ProductResponse> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
