package shoe.com.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import shoe.com.api.entity.UserEntity;
import shoe.com.api.enums.Role;
import shoe.com.api.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Tạo tài khoản Admin nếu chưa tồn tại
        if (userRepository.findByEmail("admin@pf.com").isEmpty()) {
            UserEntity admin = UserEntity.builder()
                    .email("admin@pf.com")
                    .password(passwordEncoder.encode("adminpf"))
                    .fullName("Admin PF")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Tài khoản Admin đã được tạo: admin@pf.com / adminpf");
        } else {
            System.out.println("ℹ️ Tài khoản Admin admin@pf.com đã tồn tại.");
        }
    }
}
