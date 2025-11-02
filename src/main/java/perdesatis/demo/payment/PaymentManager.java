package perdesatis.demo.payment;

import com.iyzipay.Options;
import com.iyzipay.model.*;
import com.iyzipay.request.CreatePaymentRequest;
import com.iyzipay.request.CreateRefundRequest;
import com.iyzipay.request.RetrievePaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import perdesatis.demo.adres.Address;
import perdesatis.demo.adres.AdresRepository;
import perdesatis.demo.mail.EmailMessage;
import perdesatis.demo.mail.MailService;
import perdesatis.demo.product.Product;
import perdesatis.demo.product.ProductRepository;
import perdesatis.demo.response.DataResponseMessage;
import perdesatis.demo.response.ResponseMessage;
import perdesatis.demo.siparis.Order;
import perdesatis.demo.siparis.OrderItem;
import perdesatis.demo.siparis.OrderRepository;
import perdesatis.demo.siparis.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentManager implements PaymentService {
    private final Options iyzicoOptions;
    private final TopUpSessionCache topUpSessionCache;
    private final RefundSessionCache refundSessionCache;
    private final OrderRepository orderRepository;
    private final AdresRepository adresRepository;
    private final ProductRepository productRepository;
    private final MailService mailService;

    @Override
    public ResponseMessage complete3DPayment(
            String paymentId,
            String conversationId,
            HttpServletRequest httpServletRequest) {

        log.info("3D Callback alındı - paymentId: {}, conversationId: {}", paymentId, conversationId);

        if (paymentId == null || paymentId.isEmpty() || conversationId == null || conversationId.isEmpty()) {
            log.warn("Eksik parametreler: paymentId veya conversationId boş.");
            return new ResponseMessage("Eksik parametreler gönderildi.", false);
        }

        RetrievePaymentRequest retrieveRequest = new RetrievePaymentRequest();
        retrieveRequest.setPaymentId(paymentId);
        retrieveRequest.setConversationId(conversationId);
        retrieveRequest.setLocale("tr");

        try {
            Payment payment = Payment.retrieve(retrieveRequest, iyzicoOptions);
            log.info("İyzico payment status: {}", payment.getStatus());

            if (!"success".equalsIgnoreCase(payment.getStatus())) {
                log.warn("3D ödeme başarısız: {}", payment.getErrorMessage());
                return new ResponseMessage("3D ödeme başarısız: " + payment.getErrorMessage(), false);
            }

            // ✅ Ödeme başarılı
            TopUpSessionData sessionData = topUpSessionCache.get(conversationId);
            if (sessionData == null) {
                log.error("TopUpSessionCache içinde '{}' için veri bulunamadı.", conversationId);
                return new ResponseMessage("Ödeme oturum bilgisi bulunamadı.", false);
            }

            String orderNumber = generateOrderNumber();
            
            // 🔹 Sipariş oluştur
            Order order = new Order();
            order.setOrderNumber(orderNumber);
            order.setTotalAmount(sessionData.getAmount());
            order.setStatus(OrderStatus.PAID);
            order.setCreatedAt(LocalDateTime.now());
            order.setCustomerEmail(sessionData.getUsername());
            order.setCustomerName(sessionData.getFullName() != null ? sessionData.getFullName() : "Misafir Kullanıcı");
            order.setCustomerPhone(sessionData.getPhone() != null ? sessionData.getPhone() : "Bilinmiyor");
            String paymentTransactionId = null;
            if (payment.getPaymentItems() != null && !payment.getPaymentItems().isEmpty()) {
                paymentTransactionId = payment.getPaymentItems().get(0).getPaymentTransactionId();
                order.setPaymentTransactionId(paymentTransactionId); // ✅ buraya kaydet
                log.info("İyzico PaymentTransactionId: {}", paymentTransactionId);
            }

            Address address = new Address();
            address.setFullName(order.getCustomerName());
            address.setEmail(order.getCustomerEmail());
            address.setPhone(order.getCustomerPhone());
            address.setAddressLine(sessionData.getAddress() != null ? sessionData.getAddress() : "Adres Belirtilmedi");
            address.setCity(sessionData.getCity() != null ? sessionData.getCity() : "Bilinmiyor");
            address.setDistrict("Bilinmiyor");
            address.setCountry("Türkiye");
            address.setOrder(order);

            OrderItem item = new OrderItem();
            item.setProductName("Genel Ürün");
            item.setWidth(1.0);
            item.setHeight(1.0);
            item.setPleatType("1x1");
            item.setQuantity(1);
            item.setPrice(sessionData.getAmount());
            item.setOrder(order);

            order.setAddresses(List.of(address));
            order.setOrderItems(List.of(item));

            orderRepository.save(order);

            // 📌 İADE BİLGİLERİNİ BELLEKTE SAKLA
            RefundSessionData refundData = new RefundSessionData();
            refundData.setPaymentId(paymentId);
            refundData.setConversationId(conversationId);
            refundData.setFirstName(sessionData.getFullName() != null ? 
                sessionData.getFullName().split(" ")[0] : "Misafir");
            refundData.setLastName(sessionData.getFullName() != null && 
                sessionData.getFullName().split(" ").length > 1 ? 
                sessionData.getFullName().split(" ")[1] : "Kullanıcı");
            refundData.setEmail(sessionData.getUsername());
            refundData.setPaymentTransactionId(paymentTransactionId); // ✅ eklendi
            refundData.setPhone(sessionData.getPhone() != null ? sessionData.getPhone() : "");
            refundData.setAddress(sessionData.getAddress() != null ? sessionData.getAddress() : "");
            refundData.setCity(sessionData.getCity() != null ? sessionData.getCity() : "");
            refundData.setDistrict("");
            refundData.setAddressDetail(sessionData.getAddressDetail() != null ? sessionData.getAddressDetail() : "");
            refundData.setAmount(sessionData.getAmount());
            refundData.setPaymentDate(LocalDateTime.now());
            refundData.setOrderNumber(orderNumber);
            refundData.setIp(httpServletRequest.getRemoteAddr());

            refundSessionCache.put(paymentId, refundData);

            topUpSessionCache.remove(conversationId);
            sendOrderConfirmationEmail(order.getCustomerEmail(), order.getCustomerName(), orderNumber, order.getTotalAmount());

            log.info("Sipariş kaydedildi: {} - İade bilgileri bellekte saklandı", orderNumber);

            return new DataResponseMessage(
                    "Ödeme başarılı. Sipariş numaranız: " + orderNumber,
                    true,
                    orderNumber
            );

        } catch (Exception e) {
            log.error("3D ödeme tamamlama hatası:", e);
            return new ResponseMessage("3D ödeme tamamlanırken hata oluştu: " + e.getMessage(), false);
        }
    }

    private void sendOrderConfirmationEmail(String toEmail, String fullName, String orderNumber, BigDecimal totalAmount) {
        try {
            String subject = "Siparişiniz Alındı - #" + orderNumber;

            String body = """
                <html>
                <body style="font-family: Arial, sans-serif; background-color: #f8f9fa; padding: 20px;">
                    <div style="max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);">
                        <h2 style="color: #007bff;">Siparişiniz Başarıyla Alındı!</h2>
                        <p>Merhaba <b>%s</b>,</p>
                        <p>Siparişiniz başarıyla alındı. Aşağıda sipariş detaylarınızı bulabilirsiniz:</p>
                        <table style="width:100%%; border-collapse: collapse;">
                            <tr>
                                <td style="padding:8px; border-bottom:1px solid #ddd;">Sipariş Numarası:</td>
                                <td style="padding:8px; border-bottom:1px solid #ddd;"><b>#%s</b></td>
                            </tr>
                            <tr>
                                <td style="padding:8px; border-bottom:1px solid #ddd;">Toplam Tutar:</td>
                                <td style="padding:8px; border-bottom:1px solid #ddd;"><b>₺%s</b></td>
                            </tr>
                        </table>
                        <p style="margin-top:20px;">Siparişiniz kısa süre içinde hazırlanacaktır. Kargo süreci başladığında size bilgi vereceğiz.</p>
                        <p style="margin-top:20px;">Bizden alışveriş yaptığınız için teşekkür ederiz 💙</p>
                        <hr>
                        <p style="font-size:12px; color:gray;">Bu e-posta otomatik olarak gönderilmiştir. Lütfen yanıtlamayınız.</p>
                    </div>
                </body>
                </html>
                """.formatted(fullName, orderNumber, totalAmount);

            EmailMessage emailMessage = EmailMessage.builder()
                    .toEmail(toEmail)
                    .subject(subject)
                    .body(body)
                    .isHtml(true)
                    .build();

            mailService.queueEmail(emailMessage);  // ✅ mevcut mail kuyruğunu kullanır
            log.info("Sipariş onay maili gönderildi: {}", toEmail);

        } catch (Exception e) {
            log.error("Sipariş onay maili gönderilemedi: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseMessage refundPayment(RefundRequest refundRequest, HttpServletRequest httpServletRequest) {
        try {
            log.info("İade talebi alındı - paymentId: {}", refundRequest.getPaymentId());

            RefundSessionData sessionData = refundSessionCache.getByPaymentId(refundRequest.getPaymentId());

            // 1️⃣ Bellekte yoksa Order tablosundan bilgileri al
            if (sessionData == null) {
                log.warn("Bellekte refund bilgisi bulunamadı, siparişten alınacak...");

                Optional<Order> orderOpt = orderRepository.findByOrderNumber(refundRequest.getPaymentId());
                if (orderOpt.isEmpty()) {
                    log.error("OrderNumber '{}' için veri bulunamadı.", refundRequest.getPaymentId());
                    return new ResponseMessage("İade yapılacak sipariş bulunamadı.", false);
                }

                Order order = orderOpt.get();
                sessionData = new RefundSessionData();
                sessionData.setOrderNumber(order.getOrderNumber());
                sessionData.setAmount(order.getTotalAmount());
                sessionData.setFirstName(order.getCustomerName());
                sessionData.setEmail(order.getCustomerEmail());
                sessionData.setPhone(order.getCustomerPhone());
                sessionData.setAddress(order.getAddresses() != null && !order.getAddresses().isEmpty()
                        ? order.getAddresses().get(0).getAddressLine() : "Bilinmiyor");
                sessionData.setCity(order.getAddresses() != null && !order.getAddresses().isEmpty()
                        ? order.getAddresses().get(0).getCity() : "Bilinmiyor");
                sessionData.setConversationId(UUID.randomUUID().toString());
                sessionData.setPaymentTransactionId(order.getPaymentTransactionId());

                log.info("Siparişten refund bilgisi başarıyla alındı: {}", order.getOrderNumber());
            }

            // 2️⃣ İyzico transaction ID kontrolü
            if (sessionData.getPaymentTransactionId() == null || !sessionData.getPaymentTransactionId().matches("\\d+")) {
                log.error("PaymentTransactionId bulunamadı veya numerik değil: {}", sessionData.getPaymentTransactionId());
                return new ResponseMessage("İade işlemi yapılamadı: geçerli bir paymentTransactionId bulunamadı.", false);
            }

            // 3️⃣ İade tutarı kontrolü
            if (refundRequest.getRefundAmount().compareTo(sessionData.getAmount()) > 0) {
                return new ResponseMessage(
                        "İade tutarı, orijinal ödeme tutarından (" + sessionData.getAmount() + " TL) fazla olamaz.",
                        false
                );
            }

            // 4️⃣ İyzico'ya iade isteği gönder
            CreateRefundRequest request = new CreateRefundRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(sessionData.getConversationId());
            request.setPaymentTransactionId(sessionData.getPaymentTransactionId()); // ✅ numeric ID kullanılıyor
            request.setPrice(refundRequest.getRefundAmount());
            request.setIp(refundRequest.getIp() != null ? refundRequest.getIp() :
                    (httpServletRequest != null ? httpServletRequest.getRemoteAddr() : "127.0.0.1"));
            request.setCurrency(Currency.TRY.name());

            log.info("İyzico iade isteği gönderiliyor... TxId: {}", sessionData.getPaymentTransactionId());
            Refund refund = Refund.create(request, iyzicoOptions);

            if ("success".equalsIgnoreCase(refund.getStatus())) {
                log.info("İade başarılı: {} TL, TransactionId: {}", refundRequest.getRefundAmount(), sessionData.getPaymentTransactionId());

                // Sipariş durumunu güncelle
                orderRepository.findByOrderNumber(sessionData.getOrderNumber())
                        .ifPresent(order -> {
                            order.setStatus(OrderStatus.REFUNDED);
                            order.setRefundedAt(LocalDateTime.now());
                            orderRepository.save(order);
                        });

                return new DataResponseMessage<>(
                        "İade işlemi başarılı. " + refundRequest.getRefundAmount() + " TL iade edildi.",
                        true,
                        String.format("Müşteri: %s, Email: %s, Telefon: %s",
                                sessionData.getFirstName(), sessionData.getEmail(), sessionData.getPhone())
                );
            } else {
                log.warn("İade başarısız: {}", refund.getErrorMessage());
                return new ResponseMessage("İade işlemi başarısız: " + refund.getErrorMessage(), false);
            }

        } catch (Exception e) {
            log.error("İade işlemi hatası:", e);
            return new ResponseMessage("İade işlemi sırasında hata oluştu: " + e.getMessage(), false);
        }
    }


    public static String generateOrderNumber() {
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = (int) (Math.random() * 9000) + 1000;
        return "ORD-" + datePart + "-" + randomPart;
    }

    @Override
    @Transactional
    public ResponseMessage paymentAsGuest(PaymentRequest paymentRequest) {
        try {
            log.info("Gelen ödeme isteği: {}", paymentRequest);

            // 1️⃣ Ürünlerin fiyatını ve toplam tutarı hesapla
            BigDecimal toplamTutar = BigDecimal.ZERO;

            for (OrderDetail detail : paymentRequest.getOrderDetails()) {

                // Veritabanından ürün bul
                Product product = productRepository.findById(detail.getProductId())
                        .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + detail.getProductId()));

                // En (cm -> m)
                double metreCinsindenEn = detail.getWidth() / 100.0;

                // Pile çarpanı (örnek: "1x3" veya "pilesiz")
                double pileCarpani = 1.0;
                try {
                    if (detail.getPleatType() != null && !detail.getPleatType().equalsIgnoreCase("pilesiz")) {
                        String cleaned = detail.getPleatType().replace("x", "");
                        pileCarpani = Double.parseDouble(cleaned);
                    }
                } catch (Exception e) {
                    log.warn("PleatType hatalı formatta: {}", detail.getPleatType());
                }

                // Tek ürün fiyatı = metre * pile * ürün.metreFiyatı
                BigDecimal birimFiyat = BigDecimal
                        .valueOf(metreCinsindenEn * pileCarpani)
                        .multiply(product.getPrice());

                // Adetle çarp
                BigDecimal urunToplam = birimFiyat.multiply(BigDecimal.valueOf(detail.getQuantity()));

                // OrderDetail objesine fiyat ata
                detail.setPrice(urunToplam);

                // Genel toplamı artır
                toplamTutar = toplamTutar.add(urunToplam);
            }

            // Toplam tutarı request'e yaz (artık 0 değil)
            paymentRequest.setAmount(toplamTutar);

            log.info("Toplam hesaplanan tutar: {} TL", toplamTutar);

            if (toplamTutar.compareTo(BigDecimal.valueOf(20)) < 0) {
                return new ResponseMessage("Toplam tutar minimum 20 TL olmalıdır.", false);
            }

            // 2️⃣ Kart bilgilerini hazırla
            PaymentCard paymentCard = new PaymentCard();
            paymentCard.setCardHolderName(paymentRequest.getFirstName() + " " + paymentRequest.getLastName());
            paymentCard.setCardNumber(paymentRequest.getCardNumber());
            paymentCard.setExpireMonth(paymentRequest.getCardExpiry().split("/")[0].trim());
            paymentCard.setExpireYear("20" + paymentRequest.getCardExpiry().split("/")[1].trim());
            paymentCard.setCvc(paymentRequest.getCardCvc());
            paymentCard.setRegisterCard(0);

            // 3️⃣ Buyer bilgileri
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            Buyer buyer = new Buyer();
            buyer.setId(UUID.randomUUID().toString());
            buyer.setName(paymentRequest.getFirstName());
            buyer.setSurname(paymentRequest.getLastName());
            buyer.setGsmNumber(paymentRequest.getPhone());
            buyer.setEmail(paymentRequest.getEmail());
            buyer.setIdentityNumber("00000000000");
            buyer.setLastLoginDate(LocalDateTime.now().format(formatter));
            buyer.setRegistrationDate(LocalDateTime.now().format(formatter));
            buyer.setRegistrationAddress(paymentRequest.getAddress());
            buyer.setIp("0.0.0.0");
            buyer.setCity(paymentRequest.getCity());
            buyer.setCountry("Turkey");
            buyer.setZipCode("34000");

            // 4️⃣ Adres bilgileri
            com.iyzipay.model.Address address = new com.iyzipay.model.Address();
            address.setContactName(paymentRequest.getFirstName() + " " + paymentRequest.getLastName());
            address.setCity(paymentRequest.getCity());
            address.setCountry("Turkey");
            address.setAddress(paymentRequest.getAddress() +
                    (paymentRequest.getAddressDetail() != null ? " - " + paymentRequest.getAddressDetail() : ""));
            address.setZipCode("34000");

            // 5️⃣ Sepet detaylarını oluştur
            List<BasketItem> basketItems = new ArrayList<>();
            int index = 1;
            for (OrderDetail detail : paymentRequest.getOrderDetails()) {
                BasketItem item = new BasketItem();
                item.setId("ITEM-" + index++);
                item.setName(detail.getProductName());
                item.setCategory1("Perde");
                item.setCategory2(detail.getPleatType());
                item.setItemType(BasketItemType.PHYSICAL.name());
                item.setPrice(detail.getPrice());
                basketItems.add(item);
            }

            // 6️⃣ Ödeme isteği
            String conversationId = UUID.randomUUID().toString();

            CreatePaymentRequest request = new CreatePaymentRequest();
            request.setLocale(Locale.TR.getValue());
            request.setConversationId(conversationId);
            request.setPrice(toplamTutar);
            request.setPaidPrice(toplamTutar);
            request.setCurrency(Currency.TRY.name());
            request.setInstallment(1);
            request.setBasketId("ORDER-" + conversationId);
            request.setPaymentChannel(PaymentChannel.WEB.name());
            request.setPaymentGroup(PaymentGroup.PRODUCT.name());

            String baseUrl = "https://perde-satis-1.onrender.com/api";
            request.setCallbackUrl(baseUrl + "/payment/3d-callback");
            request.setPaymentCard(paymentCard);
            request.setBuyer(buyer);
            request.setShippingAddress(address);
            request.setBillingAddress(address);
            request.setBasketItems(basketItems);

            // 7️⃣ 3D başlat
            ThreedsInitialize threedsInitialize = ThreedsInitialize.create(request, iyzicoOptions);

            if ("success".equalsIgnoreCase(threedsInitialize.getStatus())) {
                // Belleğe tüm müşteri bilgilerini kaydet
                TopUpSessionData sessionData = new TopUpSessionData();
                sessionData.setUsername(buyer.getEmail());
                sessionData.setFullName(paymentRequest.getFirstName() + " " + paymentRequest.getLastName());
                sessionData.setPhone(paymentRequest.getPhone());
                sessionData.setAddress(paymentRequest.getAddress());
                sessionData.setCity(paymentRequest.getCity());
                sessionData.setAddressDetail(paymentRequest.getAddressDetail());
                sessionData.setAmount(toplamTutar);

                topUpSessionCache.put(conversationId, sessionData);

                return new DataResponseMessage<>(
                        "3D doğrulama başlatıldı. Yönlendirme yapılıyor.",
                        true,
                        threedsInitialize.getHtmlContent()
                );
            } else {
                return new ResponseMessage(
                        "3D başlatma başarısız: " + threedsInitialize.getErrorMessage(),
                        false
                );
            }

        } catch (Exception e) {
            log.error("Ödeme hatası:", e);
            return new ResponseMessage("3D başlatma hatası: " + e.getMessage(), false);
        }
    }

}