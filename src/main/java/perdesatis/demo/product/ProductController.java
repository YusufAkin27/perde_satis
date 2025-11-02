package perdesatis.demo.product;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import perdesatis.demo.response.DataResponseMessage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    /**
     * Tüm ürünleri listele (herkes erişebilir)
     */
    @GetMapping
    public ResponseEntity<DataResponseMessage<List<Product>>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(DataResponseMessage.success("Ürünler başarıyla getirildi", products));
    }

    /**
     * Ürün detayı getir (herkes erişebilir)
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataResponseMessage<Product>> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);
        if (product.isPresent()) {
            return ResponseEntity.ok(DataResponseMessage.success("Ürün başarıyla getirildi", product.get()));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Fiyat hesapla (width, height, pleatType, price ile)
     */
    @PostMapping("/{id}/calculate-price")
    public ResponseEntity<DataResponseMessage<PriceCalculationResponse>> calculatePrice(
            @PathVariable Long id,
            @RequestBody PriceCalculationRequest request) {
        Optional<Product> productOpt = productRepository.findById(id);
        
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        
        // Request'ten gelen değerleri veya ürünün varsayılan değerlerini kullan
        Double width = request.getWidth() != null ? request.getWidth() : product.getWidth();
        Double height = request.getHeight() != null ? request.getHeight() : product.getHeight();
        String pleatType = request.getPleatType() != null ? request.getPleatType() : product.getPleatType();
        BigDecimal price = request.getPrice() != null ? request.getPrice() : product.getPrice();

        if (width == null || pleatType == null || price == null) {
            return ResponseEntity.badRequest()
                    .body(DataResponseMessage.error("Fiyat hesaplamak için width, pleatType ve price gerekli"));
        }

        // Geçici bir Product nesnesi oluştur ve fiyat hesapla
        Product tempProduct = new Product();
        tempProduct.setWidth(width);
        tempProduct.setHeight(height);
        tempProduct.setPleatType(pleatType);
        tempProduct.setPrice(price);

        BigDecimal calculatedPrice = tempProduct.fiyatHesapla();

        PriceCalculationResponse response = new PriceCalculationResponse();
        response.setProductId(id);
        response.setProductName(product.getName());
        response.setWidth(width);
        response.setHeight(height);
        response.setPleatType(pleatType);
        response.setPricePerMeter(price);
        response.setCalculatedPrice(calculatedPrice);

        return ResponseEntity.ok(DataResponseMessage.success("Fiyat başarıyla hesaplandı", response));
    }

    /**
     * Fiyat hesaplama için request DTO
     */
    @lombok.Data
    static class PriceCalculationRequest {
        private Double width;
        private Double height;
        private String pleatType;
        private BigDecimal price;
    }

    /**
     * Fiyat hesaplama sonucu DTO
     */
    @lombok.Data
    static class PriceCalculationResponse {
        private Long productId;
        private String productName;
        private Double width;
        private Double height;
        private String pleatType;
        private BigDecimal pricePerMeter;
        private BigDecimal calculatedPrice;
    }
}

