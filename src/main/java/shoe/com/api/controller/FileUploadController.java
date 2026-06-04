package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.service.FileUploadService;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String url = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(ApiResponse.success("Upload thành công", url));
    }

    @PostMapping("/multiple")
    public ResponseEntity<ApiResponse<List<String>>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<String> urls = fileUploadService.uploadMultipleFiles(files);
        return ResponseEntity.ok(ApiResponse.success("Upload thành công", urls));
    }
}
