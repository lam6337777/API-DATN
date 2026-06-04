package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.ReviewResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponse> reviews = reviewService.getProductReviews(productId, page, size);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserEntity user) {

        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        }
        if (productId == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("Thiếu productId"));
        }
        if (orderId == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("Thiếu orderId"));
        }
        if (rating == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("Thiếu rating"));
        }
        if (user.getId() == null) {
            return ResponseEntity.status(400).body(ApiResponse.error("Lỗi xác thực người dùng: ID không tồn tại"));
        }

        ReviewResponse review = reviewService.createReview(user, productId, orderId, rating, comment);
        return ResponseEntity.ok(ApiResponse.success("Đánh giá thành công", review));
    }
}
