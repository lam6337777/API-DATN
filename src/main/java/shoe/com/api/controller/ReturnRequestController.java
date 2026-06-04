package shoe.com.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.ReturnRequestRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.ReturnRequestResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.service.ReturnRequestService;
import java.util.List;

@RestController
@RequestMapping("/api/return-requests")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnRequestService returnRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> createRequest(
            @Valid @RequestBody ReturnRequestRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));

        ReturnRequestResponse response = returnRequestService.createRequest(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnRequestResponse>>> getAllRequests(
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.getAllRequests()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> getRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.getRequestById(id)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> approveRequest(
            @PathVariable Long id,
            @RequestParam String adminNote,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }

        ReturnRequestResponse response = returnRequestService.approveRequest(id, adminNote);
        return ResponseEntity.ok(ApiResponse.success("Duyệt yêu cầu thành công", response));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> rejectRequest(
            @PathVariable Long id,
            @RequestParam String adminNote,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }

        ReturnRequestResponse response = returnRequestService.rejectRequest(id, adminNote);
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu thành công", response));
    }
}
