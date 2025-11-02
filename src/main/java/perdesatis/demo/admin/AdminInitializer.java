package perdesatis.demo.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Eğer admin yoksa varsayılan admin oluştur
        if (adminRepository.count() == 0) {
            Admin admin = Admin.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@perdesatis.com")
                    .build();
            adminRepository.save(admin);
            System.out.println("Varsayılan admin oluşturuldu - Username: admin, Password: admin123");
        }
    }
}

