package com.ecommerce.orderservice.order.repository;

import com.ecommerce.orderservice.order.constant.OrderStatus;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, UUID userId);

    Optional<Order> findByIdAndTenantId(Long id, Long tenantId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    // Merchant sipariş araması: alıcı e-postası veya sipariş id'si (q = "%...%" lowercase ya da null) + opsiyonel durum.
    @Query("""
            SELECT o FROM Order o
            WHERE o.tenantId = :tenantId
              AND (:status IS NULL OR o.status = :status)
              AND (:q IS NULL OR LOWER(o.buyerEmail) LIKE :q OR CAST(o.id AS string) LIKE :q)
            ORDER BY o.createdAt DESC
            """)
    Page<Order> searchForTenant(@Param("tenantId") Long tenantId,
                                @Param("status") OrderStatus status,
                                @Param("q") String q,
                                Pageable pageable);

    // --- Platform admin ---
    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:tenantId IS NULL OR o.tenantId = :tenantId)
            """)
    Page<Order> searchForAdmin(@Param("status") OrderStatus status,
                               @Param("tenantId") Long tenantId,
                               Pageable pageable);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countByStatusGrouped();

    // GMV: iptal/iade hariç toplam ciro
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            """)
    BigDecimal totalGmv();

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

    // --- Merchant analitik: tenant-scope ciro + sipariş sayısı (iptal/iade hariç) ---
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
            WHERE o.tenantId = :tenantId AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            """)
    BigDecimal tenantRevenue(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT COUNT(o) FROM Order o
            WHERE o.tenantId = :tenantId AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            """)
    long tenantOrderCount(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT COALESCE(SUM(o.commissionAmount), 0) FROM Order o
            WHERE o.tenantId = :tenantId AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            """)
    BigDecimal tenantCommission(@Param("tenantId") Long tenantId);
}
