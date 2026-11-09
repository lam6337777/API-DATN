package shoe.com.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.dto.response.ShippingQuoteResponse;
import shoe.com.api.service.ShippingService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    // Báo giá phí ship: /api/shipping/quote?province=Hà Nội&subtotal=850000
    @GetMapping("/quote")
    public ResponseEntity<ApiResponse<ShippingQuoteResponse>> quote(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) BigDecimal subtotal) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.quote(province, subtotal)));
    }
}
