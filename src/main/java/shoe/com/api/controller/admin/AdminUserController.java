package shoe.com.api.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.AdminUpdateUserRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.UserResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.exception.ResourceNotFoundException;
import shoe.com.api.repository.UserRepository;
import shoe.com.api.service.UserService;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final shoe.com.api.service.OrderService orderService;
    private final shoe.com.api.service.ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<ApiResponse<Page<shoe.com.api.dto.response.OrderResponse>>> getUserOrders(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(id, null, page, size)));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Page<shoe.com.api.dto.response.ReviewResponse>>> getUserReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getUserReviews(id, page, size)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", userService.updateUserByAdmin(id, request)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<String>> toggleUserStatus(@PathVariable Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", "id", id));

        user.setIsActive(!user.getIsActive());
        userRepository.save(user);

        String status = user.getIsActive() ? "kích hoạt" : "vô hiệu hóa";
        return ResponseEntity.ok(ApiResponse.success("Đã " + status + " tài khoản", null));
    }
}
