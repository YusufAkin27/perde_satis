package perdesatis.demo.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double width; // cm cinsinden
    private Double height;
    private String pleatType; // 1x, 1x2, 1x2.5 gibi
    private int quantity;
    private BigDecimal price; // metre fiyatı örn. 80 TL

    public BigDecimal fiyatHesapla() {
        if (width == null || pleatType == null || price == null) {
            throw new IllegalArgumentException("Width, pleatType ve price boş olamaz.");
        }

        // 1. cm -> metre
        double metreCinsindenEn = width / 100.0;

        // 2. pile çarpanı (örnek: "1x2.5" -> 2.5)
        double pileCarpani = 1.0;
        try {
            String cleaned = pleatType.replace("x", "");
            pileCarpani = Double.parseDouble(cleaned);
        } catch (Exception e) {
            System.err.println("PleatType hatalı formatta: " + pleatType);
        }

        // 3. toplam fiyat = metre * pile * metre fiyatı
        double toplam = metreCinsindenEn * pileCarpani * price.doubleValue();

        return BigDecimal.valueOf(toplam).setScale(2, BigDecimal.ROUND_HALF_UP);
    }
}
