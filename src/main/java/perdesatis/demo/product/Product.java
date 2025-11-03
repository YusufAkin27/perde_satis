package perdesatis.demo.product;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

          double metreCinsindenEn = width / 100.0;

        double pileCarpani = 1.0;
        try {
            // "1x2.5" → ["1", "2.5"]
            String[] parts = pleatType.split("x");
            if (parts.length == 2) {
                pileCarpani = Double.parseDouble(parts[1]);
            } else {
                throw new IllegalArgumentException("PleatType formatı hatalı: " + pleatType);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("PleatType sayısal değil: " + pleatType);
        }

        // 3. Toplam fiyat = metre * pile * 1m fiyatı
        double toplam = metreCinsindenEn * pileCarpani * price.doubleValue();

        return BigDecimal.valueOf(toplam).setScale(2, RoundingMode.HALF_UP);
    }

}
