package perdesatis.demo.siparis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // Sipariş numarası ile sorgulama
    Optional<Order> findByOrderNumber(String orderNumber);
    
    // Sipariş numarası ve email ile sorgulama (müşteri için)
    Optional<Order> findByOrderNumberAndCustomerEmail(String orderNumber, String customerEmail);
    
    // Email ile tüm siparişleri getirme
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    // Duruma göre siparişleri getirme
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    
    // Tüm siparişleri tarih sırasına göre getirme
    List<Order> findAllByOrderByCreatedAtDesc();
    
    // İptal edilmiş siparişler
    List<Order> findByCancelledAtIsNotNullOrderByCreatedAtDesc();
    
    // İade talep edilmiş siparişler
    @Query("SELECT o FROM Order o WHERE o.status = 'REFUND_REQUESTED' ORDER BY o.createdAt DESC")
    List<Order> findRefundRequestedOrders();
}