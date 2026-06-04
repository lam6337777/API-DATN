package shoe.com.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.dto.request.OrderRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.OrderResponse;
import shoe.com.api.enums.OrderStatus;
import shoe.com.api.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserEntity user,
            HttpServletRequest httpRequest) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        OrderResponse order = orderService.createOrder(user.getId(), request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Đặt hàng thành công", order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getUserOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        Page<OrderResponse> orders = orderService.getUserOrders(user.getId(), status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(@PathVariable String code) {
        OrderResponse order = orderService.getOrderByCode(code);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PutMapping("/{code}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable String code,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null)
            return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập"));
        OrderResponse order = orderService.cancelOrder(user.getId(), code);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", order));
    }
}
