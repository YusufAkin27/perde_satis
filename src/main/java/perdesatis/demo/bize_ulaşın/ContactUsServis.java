package perdesatis.demo.bize_ulaşın;

import perdesatis.demo.response.ResponseMessage;

public interface ContactUsServis {
    ResponseMessage gonder(ContactUsMessage message);
    ResponseMessage verifyEmail(String verificationData);
    ResponseMessage getAllMessages();
    ResponseMessage respondToMessage(Long messageId, String response);
}