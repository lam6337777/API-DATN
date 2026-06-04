package shoe.com.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    // Email của người dùng cần đặt lại mật khẩu
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    // Mã OTP xác thực (6 chữ số)
    @NotBlank(message = "Mã xác thực không được để trống")
    private String otpCode;

    // Mật khẩu mới
    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;
}
