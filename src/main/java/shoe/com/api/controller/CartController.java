package shoe.com.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.dto.request.CartRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.CartItemResponse;
import shoe.com.api.service.CartService;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart(@AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        List<CartItemResponse> items = cartService.getCartItems(user.getId());
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            @Valid @RequestBody CartRequest request,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        CartItemResponse item = cartService.addToCart(user.getId(), request.getVariantId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.success("Đã thêm vào giỏ hàng", item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItem(
            @PathVariable Long id,
            @RequestParam Integer quantity,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        CartItemResponse item = cartService.updateCartItem(user.getId(), id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật giỏ hàng", item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeCartItem(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        cartService.removeCartItem(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa khỏi giỏ hàng", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        cartService.clearCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã xóa toàn bộ giỏ hàng", null));
    }
}
