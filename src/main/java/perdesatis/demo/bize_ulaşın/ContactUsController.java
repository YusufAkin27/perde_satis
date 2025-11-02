package perdesatis.demo.bize_ulaşın;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.response.ResponseMessage;

@RestController
@RequestMapping("/bize_ulaşın")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContactUsController {
    private final ContactUsServis bizeUlasinServis;


    // Admin mesaja yanıt verir (eski endpoint - yeni AdminMessageController'da)
    @PostMapping("/admin/yanıtla/{messageId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Deprecated
    public ResponseMessage respondToMessage(
            @PathVariable Long messageId, 
            @RequestBody AdminResponseRequest request) {
        return bizeUlasinServis.respondToMessage(messageId, request.getResponse());
    }
    // Admin tüm mesajları görür (eski endpoint - yeni AdminMessageController'da)
    @GetMapping("/admin/mesajlar")
    @PreAuthorize("hasRole('ADMIN')")
    @Deprecated
    public ResponseMessage getAllMessages() {
        return bizeUlasinServis.getAllMessages();
    }

    // Kullanıcı mesaj gönderir
    @PostMapping("/gönder")
    public ResponseMessage gonder(@RequestBody ContactUsMessage message) {
        return bizeUlasinServis.gonder(message);
    }

    // Kullanıcı email doğrulama kodunu girer
    @PostMapping("/verify-email")
    public ResponseMessage verifyEmail(@RequestBody String verificationData) {
        return bizeUlasinServis.verifyEmail(verificationData);
    }



}