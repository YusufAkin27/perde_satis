package perdesatis.demo.mail;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final EmailQueue emailQueue;
    private final ObjectMapper objectMapper;

    @Value("${spring.mail.username}")
    private String senderEmail;

    // Kuyruğa ekle
    public void queueEmail(EmailMessage emailMessage) {
        emailQueue.enqueue(emailMessage);
    }

    // 1 saniyede bir çalışsın
    @Scheduled(fixedRate = 1000)
    public void sendQueuedEmails() {
        try {
            long queueSize = emailQueue.size();

            // Eğer kuyrukta 10.000'den fazla mail varsa temizle
            if (queueSize > 10000) {
                log.warn("Mail kuyruğu çok büyük ({}), temizleniyor.", queueSize);
                emailQueue.clear();
                return;
            }

            // Kuyrukta mail varsa gönder
            if (queueSize > 0) {
                int maxBatchSize = 20;  // Aynı anda max 20 mail gönder
                List<EmailMessage> batch = new ArrayList<>();

                for (int i = 0; i < maxBatchSize; i++) {
                    String emailJson = emailQueue.dequeue();
                    if (emailJson == null) break;

                    try {
                        EmailMessage email = objectMapper.readValue(emailJson, EmailMessage.class);
                        batch.add(email);
                    } catch (Exception e) {
                        log.error("Kuyruktan email deserialize hatası: {}", e.getMessage());
                    }
                }

                for (EmailMessage email : batch) {
                    sendEmail(email);
                }
            }
        } catch (Exception e) {
            log.error("Mail gönderim işlemi sırasında hata: {}", e.getMessage());
        }
    }

    private void sendEmail(EmailMessage email) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(email.getToEmail());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), email.isHtml());
            helper.setFrom(senderEmail);

            mailSender.send(mimeMessage);
            log.info("📧 E-posta başarıyla gönderildi: {}", email.getToEmail());

        } catch (MessagingException e) {
            log.error("E-posta hazırlanırken hata: {}", e.getMessage());
        } catch (Exception e) {
            log.error("E-posta gönderilirken hata: {}", e.getMessage());
        }
    }

}
