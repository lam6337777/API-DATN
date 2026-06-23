package shoe.com.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
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
        if (userRepository.findByEmail("admin@pf2.com").isEmpty()) {
            UserEntity admin = UserEntity.builder()
                    .email("admin@pf2.com")
                    .password(passwordEncoder.encode("adminpf2"))
                    .fullName("Admin PF2")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Tài khoản Admin đã được tạo: admin@pf2.com / adminpf2");
        } else {
            System.out.println("ℹ️ Tài khoản Admin admin@pf2.com đã tồn tại.");
        }
    }
}
