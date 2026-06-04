package shoe.com.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.ChangePasswordRequest;
import shoe.com.api.dto.request.UpdateProfileRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.UserResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal UserEntity user) {
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(user.getId())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        UserResponse updatedUser = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updatedUser));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        userService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}
