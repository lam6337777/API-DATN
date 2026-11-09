package shoe.com.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import shoe.com.api.dto.request.AddressRequest;
import shoe.com.api.dto.response.ApiResponse;
import shoe.com.api.entity.AddressEntity;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.service.AddressService;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressEntity>>> getMyAddresses(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(ApiResponse.success(addressService.getMyAddresses(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressEntity>> create(@Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(ApiResponse.success("Đã thêm địa chỉ", addressService.create(user, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressEntity>> update(@PathVariable Long id,
            @Valid @RequestBody AddressRequest request,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật địa chỉ", addressService.update(user.getId(), id, request)));
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressEntity>> setDefault(@PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(ApiResponse.success("Đã đặt làm mặc định", addressService.setDefault(user.getId(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal UserEntity user) {
        addressService.delete(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa địa chỉ", null));
    }
}
