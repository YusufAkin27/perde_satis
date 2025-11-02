package perdesatis.demo.siparis;

import perdesatis.demo.response.ResponseMessage;


import java.util.List;

public interface OrderService {
    
    // Müşteri işlemleri
    ResponseMessage queryOrder(OrderQueryRequest request);
    ResponseMessage updateOrderAddress(String orderNumber, String customerEmail, OrderUpdateRequest request);
    ResponseMessage cancelOrder(String orderNumber, String customerEmail, String reason);
    ResponseMessage requestRefund(String orderNumber, String customerEmail, String reason);
    
    // Admin işlemleri
    ResponseMessage getOrderByNumber(String orderNumber);
    ResponseMessage getAllOrders();
    ResponseMessage getOrdersByStatus(OrderStatus status);
    ResponseMessage updateOrderStatus(String orderNumber, OrderStatus newStatus);
    ResponseMessage updateOrderDetailsByAdmin(String orderNumber, OrderUpdateRequest request);
    ResponseMessage addAdminNote(String orderNumber, String note);
    ResponseMessage approveRefund(String orderNumber);
    ResponseMessage rejectRefund(String orderNumber, String reason);
}