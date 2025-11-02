package perdesatis.demo.bize_ulaşın;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.response.ResponseMessage;

@RestController
@RequestMapping("/bize_ulaşın")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ContactUsController {
    private final ContactUsServis bizeUlasinServis;


    // Admin mesaja yanıt verir
    @PostMapping("/admin/yanıtla/{messageId}")
    public ResponseMessage respondToMessage(
            @PathVariable Long messageId, 
            @RequestBody String response) {
        return bizeUlasinServis.respondToMessage(messageId, response);
    }
    // Admin tüm mesajları görür (sadece doğrulanmış olanlar)
    @GetMapping("/admin/mesajlar")
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