package perdesatis.demo.admin;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import perdesatis.demo.cloudinary.MediaUploadService;
import perdesatis.demo.product.Product;
import perdesatis.demo.product.ProductRepository;
import perdesatis.demo.response.DataResponseMessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Admin ürün yönetimi endpoint'leri
 * Tüm işlemler admin yetkisi gerektirir
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final MediaUploadService mediaUploadService;

    /**
     * Yeni ürün oluştur
     * POST /api/admin/products
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponseMessage<Product>> createProduct(
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "width", required = false) Double width,
            @RequestParam(value = "height", required = false) Double height,
            @RequestParam(value = "pleatType", required = false) String pleatType,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "detailImages", required = false) List<MultipartFile> detailImages
    ) {
        try {
            // Ürün oluştur
            Product product = new Product();
            product.setName(name);
            product.setPrice(price);
            product.setDescription(description);
            product.setWidth(width);
            product.setHeight(height);
            product.setPleatType(pleatType);
            product.setQuantity(quantity);

            // Kapak resmi yükle
            if (coverImage != null && !coverImage.isEmpty()) {
                String coverImageUrl = mediaUploadService.uploadAndOptimizeImage(coverImage);
                product.setCoverImageUrl(coverImageUrl);
            }

            // Detay resimleri yükle
            if (detailImages != null && !detailImages.isEmpty()) {
                List<String> imageUrls = new ArrayList<>();
                for (MultipartFile image : detailImages) {
                    if (image != null && !image.isEmpty()) {
                        String url = mediaUploadService.uploadAndOptimizeImage(image);
                        imageUrls.add(url);
                    }
                }
                product.setDetailImageUrls(imageUrls);
            }

            // Veritabanına kaydet
            Product saved = productRepository.save(product);
            return ResponseEntity.ok(DataResponseMessage.success("Ürün başarıyla oluşturuldu.", saved));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(DataResponseMessage.error("Ürün oluşturulurken hata oluştu: " + e.getMessage()));
        }
    }

    /**
     * Tüm ürünleri listele (admin)
     * GET /api/admin/products
     */
    @GetMapping
    public ResponseEntity<DataResponseMessage<List<Product>>> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return ResponseEntity.ok(DataResponseMessage.success("Ürünler başarıyla getirildi", products));
    }

    /**
     * Ürün detayı getir (admin)
     * GET /api/admin/products/{id}
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
     * Ürün güncelle (admin)
     * PUT /api/admin/products/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<DataResponseMessage<Product>> updateProduct(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "width", required = false) Double width,
            @RequestParam(value = "height", required = false) Double height,
            @RequestParam(value = "pleatType", required = false) String pleatType,
            @RequestParam(value = "quantity", required = false) Integer quantity,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
            @RequestParam(value = "detailImages", required = false) List<MultipartFile> detailImages
    ) {
        try {
            Optional<Product> productOpt = productRepository.findById(id);
            if (productOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Product product = productOpt.get();

            if (name != null) product.setName(name);
            if (description != null) product.setDescription(description);
            if (width != null) product.setWidth(width);
            if (height != null) product.setHeight(height);
            if (pleatType != null) product.setPleatType(pleatType);
            if (quantity != null) product.setQuantity(quantity);
            if (price != null) product.setPrice(price);

            // Kapak fotoğrafı güncelle
            if (coverImage != null && !coverImage.isEmpty()) {
                String coverImageUrl = mediaUploadService.uploadAndOptimizeImage(coverImage);
                product.setCoverImageUrl(coverImageUrl);
            }

            // Detay fotoğrafları güncelle
            if (detailImages != null && !detailImages.isEmpty()) {
                List<String> detailImageUrls = new ArrayList<>();
                for (MultipartFile detailImage : detailImages) {
                    if (detailImage != null && !detailImage.isEmpty()) {
                        String detailImageUrl = mediaUploadService.uploadAndOptimizeImage(detailImage);
                        detailImageUrls.add(detailImageUrl);
                    }
                }
                // Mevcut detay fotoğraflarını koru ve yenilerini ekle
                if (product.getDetailImageUrls() != null) {
                    detailImageUrls.addAll(0, product.getDetailImageUrls());
                }
                product.setDetailImageUrls(detailImageUrls);
            }

            Product updatedProduct = productRepository.save(product);

            return ResponseEntity.ok(DataResponseMessage.success("Ürün başarıyla güncellendi", updatedProduct));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(DataResponseMessage.error("Ürün güncellenirken hata: " + e.getMessage()));
        }
    }

    /**
     * Ürün sil (admin)
     * DELETE /api/admin/products/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponseMessage<Void>> deleteProduct(@PathVariable Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.ok(new DataResponseMessage<>("Ürün başarıyla silindi", true, null));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Ürün stok güncelle (admin)
     * PATCH /api/admin/products/{id}/stock
     */
    @PatchMapping("/{id}/stock")
    public ResponseEntity<DataResponseMessage<Product>> updateStock(
            @PathVariable Long id,
            @RequestParam("quantity") Integer quantity) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        product.setQuantity(quantity);
        Product updatedProduct = productRepository.save(product);

        return ResponseEntity.ok(DataResponseMessage.success("Stok başarıyla güncellendi", updatedProduct));
    }

    /**
     * Ürün fiyat güncelle (admin)
     * PATCH /api/admin/products/{id}/price
     */
    @PatchMapping("/{id}/price")
    public ResponseEntity<DataResponseMessage<Product>> updatePrice(
            @PathVariable Long id,
            @RequestParam("price") BigDecimal price) {
        Optional<Product> productOpt = productRepository.findById(id);
        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        product.setPrice(price);
        Product updatedProduct = productRepository.save(product);

        return ResponseEntity.ok(DataResponseMessage.success("Fiyat başarıyla güncellendi", updatedProduct));
    }
}

