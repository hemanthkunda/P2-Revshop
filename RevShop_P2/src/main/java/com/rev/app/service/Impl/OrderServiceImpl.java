package com.rev.app.service.Impl;

import com.rev.app.entity.*;
import com.rev.app.exception.BadRequestException;
import com.rev.app.exception.InsufficientStockException;
import com.rev.app.exception.ResourceNotFoundException;
import com.rev.app.repository.*;
import com.rev.app.service.Interface.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderServiceImpl implements IOrderService {

    @Autowired
    private IOrderRepository orderRepo;
    @Autowired
    private IOrderItemRepository orderItemRepo;
    @Autowired
    private IProductRepository productRepo;
    @Autowired
    private ICartRepository cartRepo;
    @Autowired
    private ICartItemRepository cartItemRepo;
    @Autowired
    private IUserRepository userRepo;
    @Autowired
    private IAddressRepository addressRepo;
    @Autowired
    private IPaymentRepository paymentRepo;
    @Autowired
    private com.rev.app.service.Interface.INotificationService notificationService;

    @Override
    @Transactional
    public Order placeOrder(Long userId, Long shippingAddressId, Long billingAddressId, String paymentMethodStr) {
        log.info("Processing order placement for user ID: {}", userId);
        User u = userRepo.findById(userId).orElseThrow(() -> {
            log.error("Failed to place order. User ID {} not found.", userId);
            return new ResourceNotFoundException("User not found");
        });
        Cart cart = cartRepo.findByUser(u).orElseThrow(() -> {
            log.error("Failed to place order. Cart missing for user ID {}.", userId);
            return new ResourceNotFoundException("Cart not found");
        });
        List<CartItem> cartItems = cartItemRepo.findByCart(cart);

        if (cartItems.isEmpty()) {
            log.warn("Failed to place order. Cart is empty for user ID {}.", userId);
            throw new BadRequestException("Cart is empty");
        }

        Address shipping = addressRepo.findById(shippingAddressId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping address not found"));
        Address billing = addressRepo.findById(billingAddressId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing address not found"));

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            BigDecimal price = item.getProduct().getDiscountedPrice() != null ? item.getProduct().getDiscountedPrice()
                    : item.getProduct().getPrice();
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Order order = new Order();
        order.setUser(u);
        order.setShippingAddress(shipping);
        order.setBillingAddress(billing);
        order.setTotalAmount(totalAmount);
        // Removed order.setStatus(Order.OrderStatus.PENDING); as status is moved to OrderItem level.

        order = orderRepo.save(order);

        for (CartItem item : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(item.getProduct());
            orderItem.setQuantity(item.getQuantity());

            BigDecimal price = item.getProduct().getDiscountedPrice() != null ? item.getProduct().getDiscountedPrice()
                    : item.getProduct().getPrice();
            orderItem.setPrice(price);
            orderItem.setStatus(OrderItem.OrderStatus.PENDING);

            orderItemRepo.save(orderItem);

            // Decrease Stock
            Product p = item.getProduct();
            if (p.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product: " + p.getName());
            }
            p.setQuantity(p.getQuantity() - item.getQuantity());
            productRepo.save(p);
        }

        // Process Payment
        Payment.PaymentMethod pm = Payment.PaymentMethod.valueOf(paymentMethodStr.toUpperCase());
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(pm);
        payment.setStatus(
                pm == Payment.PaymentMethod.COD ? Payment.PaymentStatus.PENDING : Payment.PaymentStatus.COMPLETED);

        // Mock transaction ID for non-COD
        if (pm != Payment.PaymentMethod.COD) {
            payment.setTransactionId("TXN-" + System.currentTimeMillis());
            log.debug("Generated mock transaction ID {} for non-COD payment", payment.getTransactionId());
        }

        paymentRepo.save(payment);

        // Clear Cart
        cart.getItems().clear();
        cartRepo.save(cart);

        // Send Notifications
        log.info("Notifying buyer {} about order #{}", u.getId(), order.getId());
        notificationService.sendNotification(u.getId(),
                "Your order #" + order.getId() + " has been placed successfully!");

        // Notify Sellers
        for (CartItem item : cartItems) {
            Long sellerUserId = item.getProduct().getSeller().getId();
            log.info("Notifying seller {} about product {} in order #{}", sellerUserId, item.getProduct().getName(), order.getId());
            notificationService.sendNotification(sellerUserId,
                    "New order received: #" + order.getId() + " for product " + item.getProduct().getName());
        }

        return order;
    }

    @Override
    public List<Order> getOrdersByUser(Long userId) {
        log.debug("Fetching orders for user ID: {}", userId);
        User u = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return orderRepo.findByUserOrderByCreatedAtDesc(u);
    }

    @Override
    public Order getOrderById(Long orderId) {
        log.debug("Fetching order by ID: {}", orderId);
        return orderRepo.findById(orderId).orElseThrow(() -> {
            log.error("Failed to fetch. Order ID {} not found.", orderId);
            return new ResourceNotFoundException("Order not found");
        });
    }

    @Override
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        // Legacy method kept for backward compatibility – updates the whole order.
        log.info("Updating whole order ID {} to status {} (legacy)", orderId, status);
        Order order = getOrderById(orderId);
        List<OrderItem> items = orderItemRepo.findByOrder(order);
        OrderItem.OrderStatus newStatus = OrderItem.OrderStatus.valueOf(status.toUpperCase());

        for (OrderItem item : items) {
            restoreStockIfCancelled(item, newStatus);
            item.setStatus(newStatus);
            orderItemRepo.save(item);
        }
        notificationService.sendNotification(
                order.getUser().getId(),
                "Your order #" + order.getId() + " status has been updated to " + status.toUpperCase());
    }

    /**
     * Update the status of a single OrderItem.
     */
    @Transactional
    public void updateOrderItemStatus(Long orderItemId, String status) {
        log.info("Updating OrderItem ID {} to status {}", orderItemId, status);
        OrderItem orderItem = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));

        OrderItem.OrderStatus newStatus = OrderItem.OrderStatus.valueOf(status.toUpperCase());

        restoreStockIfCancelled(orderItem, newStatus);

        orderItem.setStatus(newStatus);
        orderItemRepo.save(orderItem);

        // Notify buyer about the specific item status change
        Order order = orderItem.getOrder();
        log.info("Notifying buyer {} about item {} status update to {}", order.getUser().getId(), orderItem.getId(), status);
        notificationService.sendNotification(
                order.getUser().getId(),
                "Your order item #" + orderItem.getId() + " status has been updated to " + status.toUpperCase());
    }
    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);
        List<OrderItem> items = orderItemRepo.findByOrder(order);
        
        boolean allAlreadyCancelled = items.stream().allMatch(i -> i.getStatus() == OrderItem.OrderStatus.CANCELLED);
        if (allAlreadyCancelled && !items.isEmpty()) {
            throw new BadRequestException("Order/All items already cancelled");
        }

        for (OrderItem item : items) {
            if (item.getStatus() == OrderItem.OrderStatus.PENDING || item.getStatus() == OrderItem.OrderStatus.PROCESSING) {
                restoreStockIfCancelled(item, OrderItem.OrderStatus.CANCELLED);
                item.setStatus(OrderItem.OrderStatus.CANCELLED);
                orderItemRepo.save(item);
            }
        }

        notificationService.sendNotification(
                order.getUser().getId(),
                "Your order #" + order.getId() + " (all items) has been cancelled."
        );
    }


    @Override
    @Transactional
    public void returnOrder(Long orderId) {
        Order order = getOrderById(orderId);
        List<OrderItem> items = orderItemRepo.findByOrder(order);

        boolean anyDelivered = items.stream().anyMatch(i -> i.getStatus() == OrderItem.OrderStatus.DELIVERED);
        if (!anyDelivered) {
            throw new BadRequestException("Return allowed only if at least one item is delivered");
        }

        // Validate 7-day return window from order creation
        long daysSinceOrder = ChronoUnit.DAYS.between(order.getCreatedAt(), LocalDateTime.now());
        if (daysSinceOrder > 7) {
            throw new BadRequestException("Return period has expired. Items can only be returned within 7 days.");
        }

        for (OrderItem item : items) {
            if (item.getStatus() == OrderItem.OrderStatus.DELIVERED) {
                item.setStatus(OrderItem.OrderStatus.RETURN_REQUESTED);
                orderItemRepo.save(item);
            }
        }

        notificationService.sendNotification(
                order.getUser().getId(),
                "Return request submitted for items in order #" + order.getId() + ". Pending seller approval."
        );
    }

    @Override
    @Transactional
    public void returnOrderItem(Long orderItemId) {
        log.info("Processing return request for OrderItem ID: {}", orderItemId);
        OrderItem item = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));

        if (item.getStatus() != OrderItem.OrderStatus.DELIVERED) {
            throw new BadRequestException("Item must be delivered before it can be returned.");
        }

        long daysSinceOrder = ChronoUnit.DAYS.between(item.getOrder().getCreatedAt(), LocalDateTime.now());
        if (daysSinceOrder > 7) {
            throw new BadRequestException("Return period has expired for this item.");
        }

        item.setStatus(OrderItem.OrderStatus.RETURN_REQUESTED);
        orderItemRepo.save(item);

        notificationService.sendNotification(
                item.getOrder().getUser().getId(),
                "Return request submitted for " + item.getProduct().getName() + ". Pending seller approval."
        );
    }

    @Override
    @Transactional
    public void cancelReturnRequest(Long orderItemId) {
        log.info("Cancelling return request for OrderItem ID: {}", orderItemId);
        OrderItem item = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));

        if (item.getStatus() != OrderItem.OrderStatus.RETURN_REQUESTED) {
            throw new BadRequestException("Item is not in a return requested state.");
        }

        item.setStatus(OrderItem.OrderStatus.DELIVERED);
        orderItemRepo.save(item);

        notificationService.sendNotification(
                item.getOrder().getUser().getId(),
                "Return request cancelled for " + item.getProduct().getName() + ". Status reverted to Delivered."
        );
    }

    @Override
    @Transactional
    public void cancelOrderItem(Long orderItemId) {
        log.info("Cancelling OrderItem ID: {}", orderItemId);
        OrderItem item = orderItemRepo.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));

        if (item.getStatus() != OrderItem.OrderStatus.PENDING && item.getStatus() != OrderItem.OrderStatus.PROCESSING) {
            throw new BadRequestException("Item cannot be cancelled in its current status: " + item.getStatus());
        }

        restoreStockIfCancelled(item, OrderItem.OrderStatus.CANCELLED);

        item.setStatus(OrderItem.OrderStatus.CANCELLED);
        orderItemRepo.save(item);

        notificationService.sendNotification(
                item.getOrder().getUser().getId(),
                "Your order item " + item.getProduct().getName() + " has been cancelled."
        );
    }

    private void restoreStockIfCancelled(OrderItem item, OrderItem.OrderStatus newStatus) {
        // Restore stock and adjust order total if transitioning to CANCELLED, RETURNED, or REFUNDED from an active state
        if ((newStatus == OrderItem.OrderStatus.CANCELLED || newStatus == OrderItem.OrderStatus.RETURNED || newStatus == OrderItem.OrderStatus.REFUNDED)
                && item.getStatus() != OrderItem.OrderStatus.CANCELLED
                && item.getStatus() != OrderItem.OrderStatus.RETURNED
                && item.getStatus() != OrderItem.OrderStatus.REFUNDED) {
            
            // Restore Stock
            Product p = item.getProduct();
            p.setQuantity(p.getQuantity() + item.getQuantity());
            productRepo.save(p);
            log.info("Restored stock for product {} by {} units. New stock: {}", p.getName(), item.getQuantity(), p.getQuantity());

            // Adjust Order Total
            Order order = item.getOrder();
            BigDecimal itemTotal = item.getPrice().multiply(new BigDecimal(item.getQuantity()));
            order.setTotalAmount(order.getTotalAmount().subtract(itemTotal));
            orderRepo.save(order);
            log.info("Adjusted Order #{} total amount. Subtracted: {}. New total: {}", order.getId(), itemTotal, order.getTotalAmount());
        }
    }
}
