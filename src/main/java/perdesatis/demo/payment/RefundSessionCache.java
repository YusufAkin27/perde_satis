package perdesatis.demo.payment;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefundSessionCache {

    // PaymentId ile ödeme bilgilerini sakla
    private final Map<String, RefundSessionData> paymentSessionMap = new ConcurrentHashMap<>();
    
    // OrderNumber ile ödeme bilgilerini sakla (alternatif arama için)
    private final Map<String, RefundSessionData> orderSessionMap = new ConcurrentHashMap<>();

    public void put(String paymentId, RefundSessionData data) {
        if (paymentId != null && data != null) {
            paymentSessionMap.put(paymentId, data);
            if (data.getOrderNumber() != null) {
                orderSessionMap.put(data.getOrderNumber(), data);
            }
        }
    }

    public RefundSessionData getByPaymentId(String paymentId) {
        return paymentId != null ? paymentSessionMap.get(paymentId) : null;
    }

    public RefundSessionData getByOrderNumber(String orderNumber) {
        return orderNumber != null ? orderSessionMap.get(orderNumber) : null;
    }

    public void remove(String paymentId) {
        RefundSessionData data = paymentSessionMap.remove(paymentId);
        if (data != null && data.getOrderNumber() != null) {
            orderSessionMap.remove(data.getOrderNumber());
        }
    }

    public boolean exists(String paymentId) {
        return paymentId != null && paymentSessionMap.containsKey(paymentId);
    }
}