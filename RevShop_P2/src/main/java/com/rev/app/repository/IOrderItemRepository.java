package com.rev.app.repository;

import com.rev.app.entity.Order;
import com.rev.app.entity.OrderItem;
import com.rev.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IOrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);

    List<OrderItem> findByProduct(Product product);

    @org.springframework.data.jpa.repository.Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product p JOIN FETCH oi.order o JOIN p.seller s WHERE s.id = :sellerId")
    List<OrderItem> findByProductSellerId(@org.springframework.data.repository.query.Param("sellerId") Long sellerId);
}
