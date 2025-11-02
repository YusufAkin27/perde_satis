package perdesatis.demo.siparis;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import perdesatis.demo.adres.Address;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String orderNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerPhone;

    // İptal/İade bilgileri
    @Column(length = 500)
    private String cancelReason;

    @Column
    private LocalDateTime cancelledAt;

    @Column
    private LocalDateTime refundedAt;

    @Column(length = 1000)
    private String adminNotes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;

    // İptal edilebilir mi?
    public boolean canCancel() {
        return status == OrderStatus.PENDING || 
               status == OrderStatus.PAID || 
               status == OrderStatus.PROCESSING;
    }
    
    // İade talep edilebilir mi?
    public boolean canRefund() {
        return status == OrderStatus.SHIPPED || 
               status == OrderStatus.DELIVERED;
    }
}