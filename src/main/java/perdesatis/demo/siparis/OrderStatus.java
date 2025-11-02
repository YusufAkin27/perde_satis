package perdesatis.demo.siparis;

public enum OrderStatus {
    PENDING,           // Ödeme bekleniyor
    PAID,              // Ödeme tamamlandı
    PROCESSING,        // İşleme alındı
    SHIPPED,           // Kargoya verildi
    DELIVERED,         // Teslim edildi
    CANCELLED,         // İptal edildi
    REFUND_REQUESTED,  // İade talep edildi
    REFUNDED,          // İade yapıldı
    COMPLETED          // Tamamlandı
}