package perdesatis.demo.siparis;

import lombok.Data;

@Data
public class OrderUpdateRequest {
    
    // Müşteri bilgileri (Admin için)
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Adres güncelleme alanları
    private String fullName;
    private String phone;
    private String addressLine;
    private String addressDetail;
    private String city;
    private String district;
    private String postalCode;
}