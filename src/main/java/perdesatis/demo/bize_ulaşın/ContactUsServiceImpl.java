package perdesatis.demo.bize_ulaşın;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import perdesatis.demo.mail.EmailMessage;
import perdesatis.demo.mail.MailService;
import perdesatis.demo.response.DataResponseMessage;
import perdesatis.demo.response.ResponseMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactUsServiceImpl implements ContactUsServis {

    private final ContactUsRepository contactUsRepository;
    private final EmailVerificationRepository verificationRepository;
    private final MailService mailService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResponseMessage gonder(ContactUsMessage message) {
        try {
            // 1. Mesajı doğrulama bekleyen olarak kaydet
            ContactUs contactUs = ContactUs.builder()
                    .name(message.getName())
                    .email(message.getEmail())
                    .phone(message.getPhone())
                    .subject(message.getSubject())
                    .message(message.getMessage())
                    .verified(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            contactUsRepository.save(contactUs);

            // 2. 6 haneli doğrulama kodu üret
            String verificationCode = String.format("%06d", new Random().nextInt(999999));

            // 3. Kod bilgisini kaydet
            EmailVerificationCode code = new EmailVerificationCode();
            code.setEmail(message.getEmail());
            code.setCode(verificationCode);
            code.setUsed(false);
            code.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            code.setCreatedAt(LocalDateTime.now());
            verificationRepository.save(code);

            // 4. E-posta gönderimi
            String emailBody = buildVerificationEmail(message.getName(), verificationCode);
            EmailMessage emailMessage = EmailMessage.builder()
                    .toEmail(message.getEmail())
                    .subject("E-posta Doğrulama Kodu - Bize Ulaşın")
                    .body(emailBody)
                    .isHtml(true)
                    .build();
            mailService.queueEmail(emailMessage);


            // 5. Yanıt
            return new ResponseMessage(
                    "Mesajınız alındı. Lütfen e-posta adresinize gönderilen doğrulama kodunu girin.",
                    true
            );

        } catch (Exception e) {
            return new ResponseMessage("Mesaj gönderilemedi: " + e.getMessage(), false);
        }
    }

    @Override
    @Transactional
    public ResponseMessage verifyEmail(String verificationData) {
        try {
            // {"code":"123456"} formatında gelir
            var data = objectMapper.readTree(verificationData);
            String code = data.get("code").asText();

            // Kod geçerliliğini kontrol et (email aramıyoruz artık)
            var verificationOpt = verificationRepository
                    .findTopByCodeAndUsedFalseOrderByCreatedAtDesc(code);

            if (verificationOpt.isEmpty()) {
                return new ResponseMessage("Geçersiz doğrulama kodu!", false);
            }

            EmailVerificationCode verification = verificationOpt.get();

            // Süresi dolmuş mu kontrol et
            if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
                return new ResponseMessage("Doğrulama kodunun süresi dolmuş!", false);
            }

            // Kodu kullanıldı olarak işaretle
            verification.setUsed(true);
            verificationRepository.save(verification);

            // Koda bağlı email bilgisini al
            String email = verification.getEmail();

            // Bu email adresine ait son mesajı doğrula
            contactUsRepository.findAll().stream()
                    .filter(c -> c.getEmail().equals(email) && !c.isVerified())
                    .max((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt()))
                    .ifPresent(contact -> {
                        contact.setVerified(true);
                        contactUsRepository.save(contact);
                    });

            return new ResponseMessage(
                    "E-posta adresiniz başarıyla doğrulandı! Mesajınız en kısa sürede yanıtlanacaktır.",
                    true
            );

        } catch (Exception e) {
            return new ResponseMessage("Doğrulama işlemi başarısız: " + e.getMessage(), false);
        }
    }

    @Override
    public ResponseMessage getAllMessages() {
        try {
            List<ContactUs> messages = contactUsRepository.findAll().stream()
                    .filter(ContactUs::isVerified)
                    .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                    .toList();

            return new DataResponseMessage<>("Mesajlar başarıyla getirildi", true,messages);


        } catch (Exception e) {
            return new ResponseMessage("Mesajlar getirilemedi: " + e.getMessage(), false);

        }
    }

    @Override
    public ResponseMessage respondToMessage(Long messageId, String response) {
        try {
            var contactOpt = contactUsRepository.findById(messageId);
            
            if (contactOpt.isEmpty()) {
                return new ResponseMessage("Mesaj bulunamadı!", false);

            }

            ContactUs contact = contactOpt.get();

            String emailBody = buildResponseEmail(contact.getName(), contact.getSubject(), response);
            
            EmailMessage emailMessage = EmailMessage.builder()
                    .toEmail(contact.getEmail())
                    .subject("Re: " + contact.getSubject())
                    .body(emailBody)
                    .isHtml(true)
                    .build();

            mailService.queueEmail(emailMessage);

            return new ResponseMessage("Yanıt başarıyla gönderildi: " + contact.getEmail(), true);


        } catch (Exception e) {
            return new ResponseMessage("Yanıt gönderilemedi: " + e.getMessage(), false);

        }
    }

    private String buildVerificationEmail(String name, String code) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .code-box { background: white; border: 2px dashed #667eea; padding: 20px; text-align: center; margin: 20px 0; border-radius: 8px; }
                    .code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📧 Email Doğrulama</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba <strong>%s</strong>,</p>
                        <p>Bize ulaştığınız için teşekkür ederiz. Email adresinizi doğrulamak için aşağıdaki kodu kullanın:</p>
                        
                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>
                        
                        <p><strong>⏰ Bu kod 15 dakika geçerlidir.</strong></p>
                        <p>Eğer bu işlemi siz yapmadıysanız, bu emaili dikkate almayın.</p>
                    </div>
                    <div class="footer">
                        <p>Bu otomatik bir emaildir, lütfen yanıtlamayın.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, code);
    }
    private String buildResponseEmail(String name, String subject, String response) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .message-box { background: white; padding: 20px; border-left: 4px solid #667eea; margin: 20px 0; border-radius: 4px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💬 Mesajınıza Yanıt</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba <strong>%s</strong>,</p>
                        <p>Konusu "<strong>%s</strong>" olan mesajınıza yanıt verildi:</p>
                        
                        <div class="message-box">
                            <p>%s</p>
                        </div>
                        
                        <p>Başka sorularınız varsa bizimle iletişime geçmekten çekinmeyin.</p>
                    </div>
                    <div class="footer">
                        <p>Bu emaile yanıt verebilirsiniz.</p>
                    </div>
                </div>
            </body>
            </html>
            """, name, subject, response.replace("\n", "<br>"));
    }

}