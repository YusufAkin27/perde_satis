package perdesatis.demo.bize_ulaşın;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findTopByCodeAndUsedFalseOrderByCreatedAtDesc(String code);
}