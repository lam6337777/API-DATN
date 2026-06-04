package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import shoe.com.api.dto.request.LoginRequest;
import shoe.com.api.dto.request.RegisterRequest;
import shoe.com.api.dto.request.ForgotPasswordRequest;
import shoe.com.api.dto.request.ResetPasswordRequest;
import shoe.com.api.dto.response.UserResponse;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.exception.BadRequestException;
import shoe.com.api.repository.UserRepository;
import shoe.com.api.enums.PaymentStatus;
import shoe.com.api.entity.OrderEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    /**
     * Đăng ký tài khoản mới
     */
    public UserResponse register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();

        user = userRepository.save(user);
        String token = jwtService.generateToken(user);
        return toUserResponse(user, token);
    }

    /**
     * Đăng nhập - trả về thông tin user và JWT
     */
    public UserResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        } catch (Exception e) {
            throw new BadRequestException("Email hoặc mật khẩu không đúng");
        }

        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email hoặc mật khẩu không đúng"));

        // Kiểm tra quyền truy cập Admin
        if (request.getIsAdmin() != null && request.getIsAdmin()) {
            if (user.getRole() != shoe.com.api.enums.Role.ADMIN) {
                throw new BadRequestException("Bạn không có quyền truy cập quản trị");
            }
        }

        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }

        String token = jwtService.generateToken(user);
        return toUserResponse(user, token);
    }

    /**
     * Lấy thông tin user theo ID
     */
    public UserResponse getUserById(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy người dùng"));
        return toUserResponse(user, null);
    }

    /**
     * Yêu cầu khôi phục mật khẩu - gửi OTP qua email
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email không tồn tại trong hệ thống"));

        // Kiểm tra tài khoản hoạt động
        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }

        // Tạo mã OTP 6 chữ số ngẫu nhiên
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        
        // Thiết lập mã OTP và thời gian hết hạn (5 phút)
        user.setResetPasswordToken(otpCode);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Gửi email chứa OTP
        emailService.sendResetPasswordEmail(user.getEmail(), otpCode);
    }

    /**
     * Xác thực OTP và đặt lại mật khẩu mới
     */
    public void resetPassword(ResetPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Email không tồn tại trong hệ thống"));

        // Kiểm tra tài khoản hoạt động
        if (!user.getIsActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa");
        }

        // Kiểm tra mã OTP hợp lệ
        if (user.getResetPasswordToken() == null || !user.getResetPasswordToken().equals(request.getOtpCode())) {
            throw new BadRequestException("Mã xác thực không hợp lệ");
        }

        // Kiểm tra OTP hết hạn chưa
        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Mã xác thực đã hết hạn");
        }

        // Đổi mật khẩu mới và xóa mã OTP
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

    // Chuyển Entity sang Response DTO
    public UserResponse toUserResponse(UserEntity user, String token) {
        int totalOrders = 0;
        BigDecimal totalSpent = BigDecimal.ZERO;

        if (user.getOrders() != null) {
            totalOrders = user.getOrders().size();
            totalSpent = user.getOrders().stream()
                    .filter(order -> order.getPaymentStatus() == PaymentStatus.PAID)
                    .map(OrderEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .totalOrders(totalOrders)
                .totalSpent(totalSpent)
                .token(token)
                .build();
    }
}
