package shoe.com.api.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.CategoryEntity;
import shoe.com.api.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryEntity>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryEntity>> createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl) {
        CategoryEntity category = categoryService.createCategory(name, description, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Tạo danh mục thành công", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryEntity>> updateCategory(
            @PathVariable String id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String imageUrl) {
        CategoryEntity category = categoryService.updateCategory(id, name, description, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa danh mục", null));
    }
}
