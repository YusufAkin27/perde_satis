package perdesatis.demo.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.bize_ulaşın.AdminResponseRequest;
import perdesatis.demo.bize_ulaşın.ContactUs;
import perdesatis.demo.bize_ulaşın.ContactUsRepository;
import perdesatis.demo.mail.EmailMessage;
import perdesatis.demo.mail.MailService;
import perdesatis.demo.response.DataResponseMessage;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMessageController {

    private final ContactUsRepository contactUsRepository;
    private final MailService mailService;

    @GetMapping
    public ResponseEntity<DataResponseMessage<List<ContactUs>>> getAllMessages() {
        List<ContactUs> messages = contactUsRepository.findAll().stream()
                .filter(ContactUs::isVerified)
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponseMessage.success("Mesajlar başarıyla getirildi", messages));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponseMessage<ContactUs>> getMessageById(@PathVariable Long id) {
        return contactUsRepository.findById(id)
                .map(message -> ResponseEntity.ok(DataResponseMessage.success("Mesaj başarıyla getirildi", message)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<DataResponseMessage<Void>> respondToMessage(
            @PathVariable Long id,
            @RequestBody AdminResponseRequest request) {
        try {
            ContactUs contact = contactUsRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Mesaj bulunamadı"));

            String emailBody = buildResponseEmail(contact.getName(), contact.getSubject(), request.getResponse());
            
            EmailMessage emailMessage = EmailMessage.builder()
                    .toEmail(contact.getEmail())
                    .subject("Re: " + contact.getSubject())
                    .body(emailBody)
                    .isHtml(true)
                    .build();

            mailService.queueEmail(emailMessage);

            return ResponseEntity.ok(new DataResponseMessage<>(
                    "Yanıt başarıyla gönderildi: " + contact.getEmail(),
                    true,
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(DataResponseMessage.error("Yanıt gönderilemedi: " + e.getMessage()));
        }
    }

    @PostMapping("/broadcast")
    public ResponseEntity<DataResponseMessage<Void>> sendBroadcastMessage(
            @RequestBody BroadcastMessageRequest request) {
        try {
            List<ContactUs> verifiedContacts = contactUsRepository.findAll().stream()
                    .filter(ContactUs::isVerified)
                    .collect(Collectors.toList());

            String emailBody = buildBroadcastEmail(request.getSubject(), request.getMessage());

            for (ContactUs contact : verifiedContacts) {
                EmailMessage emailMessage = EmailMessage.builder()
                        .toEmail(contact.getEmail())
                        .subject(request.getSubject())
                        .body(emailBody.replace("{{name}}", contact.getName()))
                        .isHtml(true)
                        .build();
                mailService.queueEmail(emailMessage);
            }

            return ResponseEntity.ok(new DataResponseMessage<>(
                    "Toplu mesaj " + verifiedContacts.size() + " kişiye gönderildi",
                    true,
                    null
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(DataResponseMessage.error("Toplu mesaj gönderilemedi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponseMessage<Void>> deleteMessage(@PathVariable Long id) {
        if (contactUsRepository.existsById(id)) {
            contactUsRepository.deleteById(id);
            return ResponseEntity.ok(new DataResponseMessage<>("Mesaj başarıyla silindi", true, null));
        } else {
            return ResponseEntity.notFound().build();
        }
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

    private String buildBroadcastEmail(String subject, String message) {
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
                        <h1>📢 %s</h1>
                    </div>
                    <div class="content">
                        <p>Merhaba <strong>{{name}}</strong>,</p>
                        
                        <div class="message-box">
                            <p>%s</p>
                        </div>
                        
                        <p>Teşekkür ederiz.</p>
                    </div>
                    <div class="footer">
                        <p>Bu emaile yanıt verebilirsiniz.</p>
                    </div>
                </div>
            </body>
            </html>
            """, subject, message.replace("\n", "<br>"));
    }

    @Data
    static class BroadcastMessageRequest {
        private String subject;
        private String message;
    }
}

