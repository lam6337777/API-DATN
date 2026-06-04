package shoe.com.api.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.ProductRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.ProductResponse;
import shoe.com.api.entity.ProductEntity;
import shoe.com.api.repository.ProductRepository;
import shoe.com.api.service.DataImportService;
import shoe.com.api.service.ProductService;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final DataImportService dataImportService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductEntity>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductEntity> products = productRepository.findAll(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo sản phẩm thành công", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", product));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm", null));
    }

    @PutMapping("/{productId}/variants/{variantId}/stock")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @PathVariable String productId,
            @PathVariable Long variantId,
            @RequestParam Integer quantity) {
        productService.updateStock(productId, variantId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tồn kho thành công", null));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Integer>> importProducts(@RequestParam("file") MultipartFile file) {
        int count = dataImportService.importProductsFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Đã import thành công " + count + " sản phẩm từ Excel", count));
    }

    @PostMapping("/import-csv")
    public ResponseEntity<ApiResponse<Integer>> importProductsCsv(@RequestParam("file") MultipartFile file) {
        int count = dataImportService.importProductsFromCsv(file);
        return ResponseEntity.ok(ApiResponse.success("Đã import thành công " + count + " sản phẩm từ CSV", count));
    }
}
