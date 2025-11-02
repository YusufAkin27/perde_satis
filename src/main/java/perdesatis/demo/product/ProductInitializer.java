package perdesatis.demo.product;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ProductInitializer {

    private final ProductRepository productRepository;

    @PostConstruct
    public void initProducts() {
        // Eğer zaten ürün varsa tekrar ekleme
        if (productRepository.count() > 0) {
            return;
        }

        Product klasik = new Product();
        klasik.setName("Klasik Perde");
        klasik.setPrice(BigDecimal.valueOf(80));
        klasik.setQuantity(0);
        klasik.setPleatType(null);
        klasik.setWidth(null);
        klasik.setCoverImageUrl("https://www.google.com/url?sa=i&url=https%3A%2F%2Fwww.shutterstock.com%2Fsearch%2Finspirational-images&psig=AOvVaw1jo1M4GIGfWZyzS2wSPTbS&ust=1762176826769000&source=images&cd=vfe&opi=89978449&ved=0CBIQjRxqFwoTCPDzmrDK05ADFQAAAAAdAAAAABAE");
        klasik.setHeight(null);
        productRepository.save(klasik);

        Product dikeyStor = new Product();
        dikeyStor.setName("Dikey Stor Perde - Beyaz");
        dikeyStor.setPrice(BigDecimal.valueOf(70));
        dikeyStor.setQuantity(0);
        dikeyStor.setPleatType(null);
        dikeyStor.setWidth(null);
        dikeyStor.setHeight(null);
        productRepository.save(dikeyStor);

        Product zebra = new Product();
        zebra.setName("Zebra Perde - Bej");
        zebra.setPrice(BigDecimal.valueOf(50));
        zebra.setQuantity(0);
        zebra.setPleatType(null);
        zebra.setWidth(null);
        zebra.setHeight(null);
        productRepository.save(zebra);

        Product jaluzi = new Product();
        jaluzi.setName("Jaluzi Perde - Ahşap");
        jaluzi.setPrice(BigDecimal.valueOf(80));
        jaluzi.setQuantity(0);
        jaluzi.setPleatType(null);
        jaluzi.setWidth(null);
        jaluzi.setHeight(null);
        productRepository.save(jaluzi);

        System.out.println("✅ 4 ürün başarıyla eklendi.");
    }
}
