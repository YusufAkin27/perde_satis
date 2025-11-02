package perdesatis.demo.siparis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import perdesatis.demo.siparis.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponseDTO {
    
    private Long id;
    private String orderNumber;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private String statusDescription;
    
    // Müşteri bilgileri
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Adres bilgileri
    private List<AddressDTO> addresses;
    
    // Sipariş kalemleri
    private List<OrderItemDTO> orderItems;
    
    // İptal/İade bilgileri
    private boolean canCancel;
    private boolean canRefund;
    private String cancelReason;
    private LocalDateTime cancelledAt;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressDTO {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String addressLine;
        private String addressDetail;
        private String city;
        private String district;
        private String postalCode;
        private String country;
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItemDTO {
        private Long id;
        private String productName;
        private Double width;
        private Double height;
        private String pleatType;
        private int quantity;
        private BigDecimal price;
    }
}