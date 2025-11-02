package perdesatis.demo.adres;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import perdesatis.demo.siparis.Order;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;  // örnek: Yusuf Akın

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false, length = 255)
    private String addressLine; // Ana adres (örnek: Atatürk Cd. No:25)

    @Column(length = 255)
    private String addressDetail; // Daire, kat vs. (opsiyonel)

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String district;

    @Column(length = 10)
    private String postalCode; // örnek: 12000

    @Column(length = 60)
    private String country = "Türkiye";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;
}
