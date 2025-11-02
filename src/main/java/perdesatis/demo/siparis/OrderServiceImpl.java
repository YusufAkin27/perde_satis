package perdesatis.demo.siparis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import perdesatis.demo.adres.Address;
import perdesatis.demo.payment.PaymentService;
import perdesatis.demo.payment.RefundRequest;
import perdesatis.demo.response.DataResponseMessage;
import perdesatis.demo.response.ResponseMessage;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Override
    public ResponseMessage queryOrder(OrderQueryRequest request) {
        log.info("Sipariş sorgulanıyor: {}", request.getOrderNumber());

        if (request.getOrderNumber() == null || request.getCustomerEmail() == null) {
            return new ResponseMessage("Sipariş numarası veya e-posta adresi eksik.", false);
        }

        return orderRepository.findByOrderNumberAndCustomerEmail(
                        request.getOrderNumber(),
                        request.getCustomerEmail()
                )
                .<ResponseMessage>map(order ->
                        new DataResponseMessage<>(
                                "Sipariş bulundu.",
                                true,
                                convertToDTO(order)
                        )
                )
                .orElseGet(() ->
                        new ResponseMessage(
                                "Sipariş bulunamadı veya email adresi eşleşmiyor.",
                                false
                        )
                );
    }
    @Override
    @Transactional
    public ResponseMessage updateOrderAddress(String orderNumber, String customerEmail, OrderUpdateRequest request) {
        log.info("Adres güncelleniyor: {}", orderNumber);

        return orderRepository.findByOrderNumberAndCustomerEmail(orderNumber, customerEmail)
                .map(order -> {
                    // Sadece belirli durumlarda adres güncellenebilir
                    if (order.getStatus() == OrderStatus.SHIPPED ||
                            order.getStatus() == OrderStatus.DELIVERED ||
                            order.getStatus() == OrderStatus.CANCELLED) {
                        return new ResponseMessage(
                                "Bu sipariş durumunda adres güncellenemez.",
                                false
                        );
                    }

                    // İlk adresi güncelle
                    if (!order.getAddresses().isEmpty()) {
                        Address address = order.getAddresses().get(0);

                        if (request.getFullName() != null) address.setFullName(request.getFullName());
                        if (request.getPhone() != null) address.setPhone(request.getPhone());
                        if (request.getAddressLine() != null) address.setAddressLine(request.getAddressLine());
                        if (request.getAddressDetail() != null) address.setAddressDetail(request.getAddressDetail());
                        if (request.getCity() != null) address.setCity(request.getCity());
                        if (request.getDistrict() != null) address.setDistrict(request.getDistrict());
                        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
                    }

                    orderRepository.save(order);
                    log.info("Adres güncellendi: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "Adres başarıyla güncellendi.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElse(new ResponseMessage("Sipariş bulunamadı.", false));
    }

    @Override
    @Transactional
    public ResponseMessage cancelOrder(String orderNumber, String customerEmail, String reason) {
        log.info("Sipariş iptal ediliyor: {}", orderNumber);

        return orderRepository.findByOrderNumberAndCustomerEmail(orderNumber, customerEmail)
                .map(order -> {
                    if (!order.canCancel()) {
                        return new ResponseMessage(
                                "Bu sipariş durumunda iptal edilemez. Sipariş durumu: " + order.getStatus(),
                                false
                        );
                    }

                    order.setStatus(OrderStatus.CANCELLED);
                    order.setCancelReason(reason);
                    order.setCancelledAt(LocalDateTime.now());
                    orderRepository.save(order);

                    log.info("Sipariş iptal edildi: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "Sipariş başarıyla iptal edildi.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElse(new ResponseMessage("Sipariş bulunamadı.", false));
    }

    @Override
    @Transactional
    public ResponseMessage requestRefund(String orderNumber, String customerEmail, String reason) {
        log.info("İade talebi oluşturuluyor: {}", orderNumber);

        return orderRepository.findByOrderNumberAndCustomerEmail(orderNumber, customerEmail)
                .map(order -> {
                    if (!order.canRefund()) {
                        return new ResponseMessage(
                                "Bu sipariş için iade talep edilemez. Sipariş durumu: " + order.getStatus(),
                                false
                        );
                    }

                    order.setStatus(OrderStatus.REFUND_REQUESTED);
                    order.setCancelReason(reason);
                    orderRepository.save(order);

                    log.info("İade talebi oluşturuldu: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "İade talebiniz alınmıştır. En kısa sürede değerlendirilecektir.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElse(new ResponseMessage("Sipariş bulunamadı.", false));
    }

    @Override
    public ResponseMessage getOrderByNumber(String orderNumber) {
        log.info("Admin sipariş sorguluyor: {}", orderNumber);

        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            return new ResponseMessage("Sipariş numarası boş olamaz.", false);
        }

        return orderRepository.findByOrderNumber(orderNumber)
                .<ResponseMessage>map(order ->
                        new DataResponseMessage<>(
                                "Sipariş bulundu.",
                                true,
                                convertToDTO(order)
                        )
                )
                .orElseGet(() ->
                        new ResponseMessage("Sipariş bulunamadı.", false)
                );
    }


    @Override
    public ResponseMessage getAllOrders() {
        log.info("Tüm siparişler getiriliyor");

        List<OrderResponseDTO> orders = orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new DataResponseMessage<>(
                "Siparişler listelendi.",
                true,
                orders
        );
    }

    @Override
    public ResponseMessage getOrdersByStatus(OrderStatus status) {
        log.info("Duruma göre siparişler getiriliyor: {}", status);

        List<OrderResponseDTO> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return new DataResponseMessage<>(
                status + " durumundaki siparişler listelendi.",
                true,
                orders
        );
    }
    @Override
    @Transactional
    public ResponseMessage updateOrderStatus(String orderNumber, OrderStatus newStatus) {
        log.info("Sipariş durumu güncelleniyor: {} -> {}", orderNumber, newStatus);

        if (orderNumber == null || newStatus == null) {
            return new ResponseMessage("Sipariş numarası veya yeni durum boş olamaz.", false);
        }

        return orderRepository.findByOrderNumber(orderNumber)
                .<ResponseMessage>map(order -> {
                    order.setStatus(newStatus);
                    orderRepository.save(order);

                    log.info("Sipariş durumu güncellendi: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "Sipariş durumu güncellendi.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElseGet(() ->
                        new ResponseMessage("Sipariş bulunamadı.", false)
                );
    }

    @Override
    @Transactional
    public ResponseMessage updateOrderDetailsByAdmin(String orderNumber, OrderUpdateRequest request) {
        log.info("Admin sipariş detaylarını güncelliyor: {}", orderNumber);

        return orderRepository.findByOrderNumber(orderNumber)
                .<ResponseMessage>map(order -> {
                    // Müşteri bilgilerini güncelle
                    if (request.getCustomerName() != null) {
                        order.setCustomerName(request.getCustomerName());
                    }
                    if (request.getCustomerEmail() != null) {
                        order.setCustomerEmail(request.getCustomerEmail());
                    }
                    if (request.getCustomerPhone() != null) {
                        order.setCustomerPhone(request.getCustomerPhone());
                    }

                    // Adres bilgilerini güncelle
                    if (!order.getAddresses().isEmpty()) {
                        Address address = order.getAddresses().get(0);

                        if (request.getFullName() != null) address.setFullName(request.getFullName());
                        if (request.getPhone() != null) address.setPhone(request.getPhone());
                        if (request.getAddressLine() != null) address.setAddressLine(request.getAddressLine());
                        if (request.getAddressDetail() != null) address.setAddressDetail(request.getAddressDetail());
                        if (request.getCity() != null) address.setCity(request.getCity());
                        if (request.getDistrict() != null) address.setDistrict(request.getDistrict());
                        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
                    }

                    orderRepository.save(order);
                    log.info("Sipariş detayları güncellendi: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "Sipariş detayları başarıyla güncellendi.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElseGet(() -> new ResponseMessage("Sipariş bulunamadı.", false));
    }


    @Override
    @Transactional
    public ResponseMessage addAdminNote(String orderNumber, String note) {
        log.info("Admin notu ekleniyor: {}", orderNumber);

        return orderRepository.findByOrderNumber(orderNumber)
                .map(order -> {
                    String existingNotes = order.getAdminNotes();
                    String newNote = LocalDateTime.now() + ": " + note;

                    if (existingNotes != null && !existingNotes.isEmpty()) {
                        order.setAdminNotes(existingNotes + "\n" + newNote);
                    } else {
                        order.setAdminNotes(newNote);
                    }

                    orderRepository.save(order);

                    return new DataResponseMessage<>(
                            "Not eklendi.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElse((DataResponseMessage<OrderResponseDTO>) new ResponseMessage("Sipariş bulunamadı.", false));
    }

    @Override
    @Transactional
    public ResponseMessage approveRefund(String orderNumber) {
        log.info("İade onaylanıyor: {}", orderNumber);

        return orderRepository.findByOrderNumber(orderNumber)
                .map(order -> {
                    if (order.getStatus() != OrderStatus.CANCELLED) {
                        return new ResponseMessage(
                                "İade talebi bekleyen bir sipariş değil.",
                                false
                        );
                    }

                    // Sipariş durumunu REFUNDED yapmadan önce ödeme servisini çağır
                    RefundRequest refundRequest = new RefundRequest();
                    refundRequest.setPaymentId(order.getOrderNumber()); // ödeme ID'si (örnek olarak orderNumber kullanılabilir)
                    refundRequest.setRefundAmount(order.getTotalAmount());
                    refundRequest.setReason("Sipariş iptali ve iade onayı");
                    refundRequest.setIp("127.0.0.1");

                    ResponseMessage refundResult = paymentService.refundPayment(refundRequest, null);

                    if (!refundResult.isSuccess()) {
                        log.warn("İade isteği başarısız: {}", refundResult.getMessage());
                        return refundResult;
                    }

                    order.setStatus(OrderStatus.REFUNDED);
                    order.setRefundedAt(LocalDateTime.now());
                    orderRepository.save(order);

                    log.info("İade onaylandı ve ödeme servisine yönlendirildi: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "İade onaylandı ve ödeme işlemi başarıyla başlatıldı.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElseGet(() -> new ResponseMessage("Sipariş bulunamadı.", false));
    }


    @Override
    @Transactional
    public ResponseMessage rejectRefund(String orderNumber, String reason) {
        log.info("İade reddediliyor: {}", orderNumber);

        return orderRepository.findByOrderNumber(orderNumber)
                .map(order -> {
                    if (order.getStatus() != OrderStatus.REFUND_REQUESTED) {
                        return new ResponseMessage(
                                "İade talebi bekleyen bir sipariş değil.",
                                false
                        );
                    }

                    order.setStatus(OrderStatus.DELIVERED);
                    order.setCancelReason(order.getCancelReason() + "\nRet Nedeni: " + reason);
                    orderRepository.save(order);

                    log.info("İade reddedildi: {}", orderNumber);

                    return new DataResponseMessage<>(
                            "İade talebi reddedildi.",
                            true,
                            convertToDTO(order)
                    );
                })
                .orElse(new ResponseMessage("Sipariş bulunamadı.", false));
    }

    private OrderResponseDTO convertToDTO(Order order) {
        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .status(order.getStatus())
                .statusDescription(getStatusDescription(order.getStatus()))
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .addresses(order.getAddresses().stream()
                        .map(this::convertAddressToDTO)
                        .collect(Collectors.toList()))
                .orderItems(order.getOrderItems().stream()
                        .map(this::convertOrderItemToDTO)
                        .collect(Collectors.toList()))
                .canCancel(order.canCancel())
                .canRefund(order.canRefund())
                .cancelReason(order.getCancelReason())
                .cancelledAt(order.getCancelledAt())
                .build();
    }

    private OrderResponseDTO.AddressDTO convertAddressToDTO(Address address) {
        return OrderResponseDTO.AddressDTO.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .email(address.getEmail())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .addressDetail(address.getAddressDetail())
                .city(address.getCity())
                .district(address.getDistrict())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .build();
    }

    private OrderResponseDTO.OrderItemDTO convertOrderItemToDTO(OrderItem item) {
        return OrderResponseDTO.OrderItemDTO.builder()
                .id(item.getId())
                .productName(item.getProductName())
                .width(item.getWidth())
                .height(item.getHeight())
                .pleatType(item.getPleatType())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }

    private String getStatusDescription(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Ödeme Bekleniyor";
            case PAID -> "Ödeme Tamamlandı";
            case PROCESSING -> "İşleme Alındı";
            case SHIPPED -> "Kargoya Verildi";
            case DELIVERED -> "Teslim Edildi";
            case CANCELLED -> "İptal Edildi";
            case REFUND_REQUESTED -> "İade Talep Edildi";
            case REFUNDED -> "İade Yapıldı";
            case COMPLETED -> "Tamamlandı";
        };
    }
}