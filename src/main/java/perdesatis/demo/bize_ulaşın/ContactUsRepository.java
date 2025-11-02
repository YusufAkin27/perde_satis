package perdesatis.demo.bize_ulaşın;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactUsRepository extends JpaRepository<ContactUs, Long> {
    // Doğrulanmış mesajları getir
    List<ContactUs> findByVerifiedTrue();
    
    // Email'e göre doğrulanmamış mesajları getir
    List<ContactUs> findByEmailAndVerifiedFalse(String email);
}