package com.rev.app.service.Interface;

import com.rev.app.entity.Order;
import java.util.List;

public interface IOrderService {
    Order placeOrder(Long userId, Long shippingAddressId, Long billingAddressId, String paymentMethod);

    List<Order> getOrdersByUser(Long userId);

    Order getOrderById(Long orderId);

    void updateOrderStatus(Long orderId, String status);
    void cancelOrder(Long orderId);
    void updateOrderItemStatus(Long orderItemId, String status);

    void returnOrder(Long orderId);
    void returnOrderItem(Long orderItemId);
    void cancelReturnRequest(Long orderItemId);
    void cancelOrderItem(Long orderItemId);
}
