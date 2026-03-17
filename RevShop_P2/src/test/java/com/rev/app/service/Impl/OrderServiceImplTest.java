package com.rev.app.service.Impl;

import com.rev.app.entity.*;
import com.rev.app.exception.BadRequestException;
import com.rev.app.exception.ResourceNotFoundException;
import com.rev.app.repository.*;
import com.rev.app.service.Interface.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock
    private IOrderRepository orderRepo;
    @Mock
    private IOrderItemRepository orderItemRepo;
    @Mock
    private IProductRepository productRepo;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private Order order;
    private OrderItem orderItem;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");

        product = new Product();
        product.setId(1L);
        product.setQuantity(10);
        product.setName("Test Product");

        order = new Order();
        order.setId(1L);
        order.setUser(user);
        order.setTotalAmount(new BigDecimal("200.00"));

        orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setPrice(new BigDecimal("100.00"));
        orderItem.setStatus(OrderItem.OrderStatus.PENDING);
    }

    @Test
    public void testCancelOrder_RestoresStock() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepo.findByOrder(order)).thenReturn(Arrays.asList(orderItem));

        orderService.cancelOrder(1L);

        assertThat(product.getQuantity()).isEqualTo(12); // 10 + 2
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderStatus.CANCELLED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO); // 200 - 200
        verify(productRepo, times(1)).save(product);
        verify(orderRepo, times(1)).save(order);
        verify(orderItemRepo, times(1)).save(orderItem);
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString());
    }

    @Test
    public void testCancelOrderItem_RestoresStock() {
        when(orderItemRepo.findById(1L)).thenReturn(Optional.of(orderItem));

        orderService.cancelOrderItem(1L);

        assertThat(product.getQuantity()).isEqualTo(12); // 10 + 2
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderStatus.CANCELLED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(productRepo, times(1)).save(product);
        verify(orderRepo, times(1)).save(order);
        verify(orderItemRepo, times(1)).save(orderItem);
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString());
    }

    @Test
    public void testUpdateOrderItemStatus_CANCELLED_RestoresStock() {
        when(orderItemRepo.findById(1L)).thenReturn(Optional.of(orderItem));

        orderService.updateOrderItemStatus(1L, "CANCELLED");

        assertThat(product.getQuantity()).isEqualTo(12); // 10 + 2
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderStatus.CANCELLED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(productRepo, times(1)).save(product);
        verify(orderRepo, times(1)).save(order);
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString());
    }

    @Test
    public void testUpdateOrderItemStatus_AlreadyCANCELLED_DoesNotRestoreStock() {
        orderItem.setStatus(OrderItem.OrderStatus.CANCELLED);
        when(orderItemRepo.findById(1L)).thenReturn(Optional.of(orderItem));

        orderService.updateOrderItemStatus(1L, "CANCELLED");

        assertThat(product.getQuantity()).isEqualTo(10); // Remains 10
        verify(productRepo, never()).save(product);
    }

    @Test
    public void testUpdateOrderItemStatus_RETURNED_RestoresStock() {
        when(orderItemRepo.findById(1L)).thenReturn(Optional.of(orderItem));

        orderService.updateOrderItemStatus(1L, "RETURNED");

        assertThat(product.getQuantity()).isEqualTo(12); // 10 + 2
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderStatus.RETURNED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(productRepo, times(1)).save(product);
        verify(orderRepo, times(1)).save(order);
    }

    @Test
    public void testUpdateOrderStatus_CANCELLED_RestoresStock() {
        when(orderRepo.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepo.findByOrder(order)).thenReturn(Arrays.asList(orderItem));

        orderService.updateOrderStatus(1L, "CANCELLED");

        assertThat(product.getQuantity()).isEqualTo(12); // 10 + 2
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderStatus.CANCELLED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(productRepo, times(1)).save(product);
        verify(orderItemRepo, times(1)).save(orderItem);
        verify(orderRepo, times(1)).save(order);
        verify(notificationService, times(1)).sendNotification(anyLong(), anyString());
    }

    @Test
    public void testUpdateOrderItemStatus_REFUNDED_RestoresStock() {
        when(orderItemRepo.findById(1L)).thenReturn(Optional.of(orderItem));

        orderService.updateOrderItemStatus(1L, "REFUNDED");

        assertThat(product.getQuantity()).isEqualTo(12); // 10 + 2
        assertThat(orderItem.getStatus()).isEqualTo(OrderItem.OrderStatus.REFUNDED);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(productRepo, times(1)).save(product);
        verify(orderRepo, times(1)).save(order);
    }
}
