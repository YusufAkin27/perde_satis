package perdesatis.demo.bize_ulaşın;

import lombok.Data;

@Data
public class ContactUsMessage {
    private String name;
    private String email;
    private String message;
    private String phone;
    private String subject;
}
