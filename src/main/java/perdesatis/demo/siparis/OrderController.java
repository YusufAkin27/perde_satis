package perdesatis.demo.siparis;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.response.ResponseMessage;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;



    /**
     * Admin sipariş detayı görüntüleme
     * GET /api/orders/admin/{orderNumber}
     */
    @GetMapping("/admin/{orderNumber}")
    public ResponseMessage getOrderByNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByNumber(orderNumber);
    }

    /**
     * Tüm siparişleri listeleme
     * GET /api/orders/admin/all
     */
    @GetMapping("/admin/all")
    public ResponseMessage getAllOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Duruma göre sipariş listeleme
     * GET /api/orders/admin/status/{status}
     */
    @GetMapping("/admin/status/{status}")
    public ResponseMessage getOrdersByStatus(@PathVariable OrderStatus status) {
        return orderService.getOrdersByStatus(status);
    }

    /**
     * Sipariş durumu güncelleme
     * PUT /api/orders/admin/{orderNumber}/status?status=SHIPPED
     */
    @PutMapping("/admin/{orderNumber}/status")
    public ResponseMessage updateOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam OrderStatus status) {
        return orderService.updateOrderStatus(orderNumber, status);
    }

    /**
     * Admin notu ekleme
     * POST /api/orders/admin/{orderNumber}/note
     */
    @PostMapping("/admin/{orderNumber}/note")
    public ResponseMessage addAdminNote(
            @PathVariable String orderNumber,
            @RequestBody String note) {
        return orderService.addAdminNote(orderNumber, note);
    }

    /**
     * İade onaylama
     * POST /api/orders/admin/{orderNumber}/approve-refund
     */
    @PostMapping("/admin/{orderNumber}/approve-refund")
    public ResponseMessage approveRefund(@PathVariable String orderNumber) {
        return orderService.approveRefund(orderNumber);
    }

    /**
     * İade reddetme
     * POST /api/orders/admin/{orderNumber}/reject-refund?reason=xxx
     */
    @PostMapping("/admin/{orderNumber}/reject-refund")
    public ResponseMessage rejectRefund(
            @PathVariable String orderNumber,
            @RequestParam String reason) {
        return orderService.rejectRefund(orderNumber, reason);
    }
    // ========== KULLANICI İŞLEMLERİ ==========

    /**
     * Sipariş sorgulama (Email + Sipariş No ile)
     * POST /api/orders/query
     */
    @PostMapping("/query")
    public ResponseMessage queryOrder(@Valid @RequestBody OrderQueryRequest request) {
        return orderService.queryOrder(request);
    }

    /**
     * Sipariş adresini güncelleme
     * PUT /api/orders/{orderNumber}/address?email=xxx
     */
    @PutMapping("/{orderNumber}/address")
    public ResponseMessage updateAddress(
            @PathVariable String orderNumber,
            @RequestParam String email,
            @Valid @RequestBody OrderUpdateRequest request) {
        return orderService.updateOrderAddress(orderNumber, email, request);
    }

    /**
     * Sipariş iptali
     * POST /api/orders/{orderNumber}/cancel?email=xxx&reason=xxx
     */
    @PostMapping("/{orderNumber}/cancel")
    public ResponseMessage cancelOrder(
            @PathVariable String orderNumber,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "Müşteri isteği") String reason) {
        return orderService.cancelOrder(orderNumber, email, reason);
    }

    /**
     * İade talebi oluşturma
     * POST /api/orders/{orderNumber}/refund?email=xxx&reason=xxx
     */
    @PostMapping("/{orderNumber}/refund")
    public ResponseMessage requestRefund(
            @PathVariable String orderNumber,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "İade talebi") String reason) {
        return orderService.requestRefund(orderNumber, email, reason);
    }

    // ========== ADMİN İŞLEMLERİ ==========


}