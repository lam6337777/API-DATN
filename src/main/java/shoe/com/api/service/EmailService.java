package shoe.com.api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email mã OTP khôi phục mật khẩu
     */
    public void sendResetPasswordEmail(String toEmail, String otpCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực khôi phục mật khẩu - Shoe Store");
        message.setText("Chào bạn,\n\n"
                + "Bạn đã yêu cầu khôi phục mật khẩu cho tài khoản của mình tại Shoe Store.\n"
                + "Mã xác thực OTP của bạn là: " + otpCode + "\n"
                + "Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n"
                + "Trân trọng,\n"
                + "Shoe Store Team.");
        
        mailSender.send(message);
    }
}
