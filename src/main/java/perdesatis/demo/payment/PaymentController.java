package perdesatis.demo.payment;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.response.DataResponseMessage;
import perdesatis.demo.response.ResponseMessage;

import java.io.IOException;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/card")
    public ResponseMessage payment(
            @Valid @RequestBody PaymentRequest paymentRequest) {
        return paymentService.paymentAsGuest(paymentRequest);
    }

    @PostMapping("/3d-callback")
    public void  complete3DPayment(
            @RequestParam(name = "paymentId", required = false) String paymentId,
            @RequestParam(name = "conversationId", required = false) String conversationId,
            HttpServletRequest httpServletRequest,
            HttpServletResponse response) throws IOException {

        ResponseMessage result = paymentService.complete3DPayment(paymentId, conversationId, httpServletRequest);
        if (result.isSuccess()) {
            String orderNumber = ((DataResponseMessage<?>) result).getData().toString();
            String redirectUrl = "http://localhost:3002/payment-success?order=" + orderNumber;
            log.info("Ödeme başarılı, kullanıcı yönlendiriliyor: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        }
        else {
            String redirectUrl = "http://localhost:3002/payment-failed";
            log.warn("Ödeme başarısız, kullanıcı yönlendiriliyor: {}", redirectUrl);
            response.sendRedirect(redirectUrl);
        }
    }

    @PostMapping("/refund")
    public ResponseMessage refundPayment(
            @Valid @RequestBody RefundRequest refundRequest,
            HttpServletRequest httpServletRequest) {
        return paymentService.refundPayment(refundRequest, httpServletRequest);
    }
}