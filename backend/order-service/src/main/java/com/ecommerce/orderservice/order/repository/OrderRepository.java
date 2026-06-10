package com.ecommerce.orderservice.order.repository;

import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, UUID userId);

    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    @Query("""
        SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
        FROM OrderItem oi
        JOIN Order o ON oi.orderId = o.id
        WHERE o.userId = :userId
        AND oi.productId = :productId
        AND o.status IN (
            com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
            com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
            com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
        )
    """)
    boolean hasPurchased(@Param("userId") UUID userId, @Param("productId") Long productId);
}
