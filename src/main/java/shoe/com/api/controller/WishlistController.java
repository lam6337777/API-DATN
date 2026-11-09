package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.ProductResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.service.WishlistService;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getWishlist(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlist(user.getId())));
    }

    // Chỉ trả về danh sách ID để FE tô trái tim trên các thẻ sản phẩm
    @GetMapping("/ids")
    public ResponseEntity<ApiResponse<List<String>>> getWishlistIds(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getProductIds(user.getId())));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> add(@PathVariable String productId,
            @AuthenticationPrincipal UserEntity user) {
        wishlistService.add(user, productId);
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào yêu thích", null));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String productId,
            @AuthenticationPrincipal UserEntity user) {
        wishlistService.remove(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi yêu thích", null));
    }
}
