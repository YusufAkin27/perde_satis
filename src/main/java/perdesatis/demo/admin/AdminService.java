package perdesatis.demo.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public Admin createAdmin(String username, String password, String email) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("Bu kullanıcı adı zaten kullanılıyor");
        }

        Admin admin = Admin.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .build();

        return adminRepository.save(admin);
    }

    public boolean validateCredentials(String username, String password) {
        Admin admin = adminRepository.findByUsername(username)
                .orElse(null);

        if (admin == null) {
            return false;
        }

        return passwordEncoder.matches(password, admin.getPassword());
    }

    public Admin findByUsername(String username) {
        return adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin bulunamadı"));
    }
}

