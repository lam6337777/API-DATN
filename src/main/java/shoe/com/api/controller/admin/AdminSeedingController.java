package shoe.com.api.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.service.DataImportService;

@RestController
@RequestMapping("/api/admin/seeding")
@RequiredArgsConstructor
public class AdminSeedingController {

    private final DataImportService dataImportService;

    @PostMapping("/products/excel")
    public ResponseEntity<ApiResponse<Integer>> seedProductsExcel(@RequestParam("file") MultipartFile file) {
        int count = dataImportService.importProductsFromExcel(file);
        return ResponseEntity.ok(ApiResponse.success("Đã seed thành công " + count + " sản phẩm từ Excel", count));
    }

    @PostMapping("/products/csv")
    public ResponseEntity<ApiResponse<Integer>> seedProductsCsv(@RequestParam("file") MultipartFile file) {
        int count = dataImportService.importProductsFromCsv(file);
        return ResponseEntity.ok(ApiResponse.success("Đã seed thành công " + count + " sản phẩm từ CSV", count));
    }
}
