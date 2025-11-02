package perdesatis.demo.payment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import perdesatis.demo.response.ResponseMessage;

public interface PaymentService {

    ResponseMessage complete3DPayment(String paymentId, String conversationId, HttpServletRequest httpServletRequest);

    ResponseMessage paymentAsGuest(@Valid PaymentRequest paymentRequest);

    ResponseMessage refundPayment(@Valid RefundRequest refundRequest, HttpServletRequest httpServletRequest);
}