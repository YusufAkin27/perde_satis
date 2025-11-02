package perdesatis.demo.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.bize_ulaşın.ContactUs;
import perdesatis.demo.bize_ulaşın.ContactUsRepository;
import perdesatis.demo.response.DataResponseMessage;
import perdesatis.demo.siparis.Order;
import perdesatis.demo.siparis.OrderRepository;
import perdesatis.demo.siparis.OrderStatus;
import perdesatis.demo.product.ProductRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ContactUsRepository contactUsRepository;

    @GetMapping("/stats")
    public ResponseEntity<DataResponseMessage<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Ürün istatistikleri
        long totalProducts = productRepository.count();
        stats.put("totalProducts", totalProducts);
        
        // Sipariş istatistikleri
        long totalOrders = orderRepository.count();
        stats.put("totalOrders", totalOrders);
        
        // Duruma göre sipariş sayıları
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByStatus(status);
            ordersByStatus.put(status.name(), count);
        }
        stats.put("ordersByStatus", ordersByStatus);
        
        // Toplam gelir (PAID, DELIVERED, COMPLETED durumundaki siparişler)
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAID || 
                           o.getStatus() == OrderStatus.DELIVERED || 
                           o.getStatus() == OrderStatus.COMPLETED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalRevenue", totalRevenue);
        
        // Bugünkü siparişler
        long todayOrders = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();
        stats.put("todayOrders", todayOrders);
        
        // Bekleyen mesajlar (doğrulanmış ama yanıtlanmamış)
        long pendingMessages = contactUsRepository.findAll().stream()
                .filter(ContactUs::isVerified)
                .count();
        stats.put("pendingMessages", pendingMessages);
        
        // Son 7 günlük sipariş trendi
        Map<String, Long> ordersLast7Days = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            long count = orderRepository.findAll().stream()
                    .filter(o -> o.getCreatedAt().toLocalDate().equals(date.toLocalDate()))
                    .count();
            ordersLast7Days.put(date.toLocalDate().toString(), count);
        }
        stats.put("ordersLast7Days", ordersLast7Days);
        
        return ResponseEntity.ok(DataResponseMessage.success("İstatistikler başarıyla getirildi", stats));
    }
}

