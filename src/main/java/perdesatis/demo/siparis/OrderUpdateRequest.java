package perdesatis.demo.siparis;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderUpdateRequest {
    
    @NotBlank(message = "Sipariş numarası boş olamaz.")
    private String orderNumber;
    
    // Adres güncelleme alanları
    private String fullName;
    private String phone;
    private String addressLine;
    private String addressDetail;
    private String city;
    private String district;
    private String postalCode;
}