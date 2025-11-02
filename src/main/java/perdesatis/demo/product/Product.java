package perdesatis.demo.product;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private Double width; // cm cinsinden
    private Double height;
    private String pleatType; // 1x, 1x2, 1x2.5 gibi
    private Integer quantity;
    private BigDecimal price; // metre fiyatı örn. 80 TL
    
    @Column(length = 1000)
    private String coverImageUrl; // Kapak fotoğrafı URL'i
    
    @ElementCollection
    @CollectionTable(name = "product_detail_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 1000)
    private List<String> detailImageUrls = new ArrayList<>(); // Ürün detay fotoğrafları

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
