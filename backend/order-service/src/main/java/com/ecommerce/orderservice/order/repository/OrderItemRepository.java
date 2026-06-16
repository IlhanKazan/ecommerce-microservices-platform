package com.ecommerce.orderservice.order.repository;

import com.ecommerce.orderservice.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    // --- Merchant analitik: ürün-bazlı satış metrikleri (iptal/iade hariç) ---
    // [productId, productName, SUM(quantity), SUM(unitPrice*quantity), COUNT(DISTINCT orderId)]
    @Query("""
            SELECT oi.productId, MAX(oi.productName), SUM(oi.quantity),
                   SUM(oi.unitPrice * oi.quantity), COUNT(DISTINCT oi.orderId)
            FROM OrderItem oi JOIN Order o ON oi.orderId = o.id
            WHERE oi.tenantId = :tenantId AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            GROUP BY oi.productId
            ORDER BY SUM(oi.unitPrice * oi.quantity) DESC
            """)
    List<Object[]> topProductsByTenant(@Param("tenantId") Long tenantId, Pageable pageable);

    // Tenant toplam satılan adet (iptal/iade hariç)
    @Query("""
            SELECT COALESCE(SUM(oi.quantity), 0)
            FROM OrderItem oi JOIN Order o ON oi.orderId = o.id
            WHERE oi.tenantId = :tenantId AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            """)
    long tenantTotalUnits(@Param("tenantId") Long tenantId);

    // --- Ürün satış metriği: verilen productId kümesi (parent + varyantlar) için kırılım ---
    // [productId, productName, SUM(quantity), SUM(unitPrice*quantity), COUNT(DISTINCT orderId)]
    // tenantId null ise (admin) tenant filtresi uygulanmaz; doluysa (merchant) o tenant'a kısıtlanır.
    @Query("""
            SELECT oi.productId, MAX(oi.productName), SUM(oi.quantity),
                   SUM(oi.unitPrice * oi.quantity), COUNT(DISTINCT oi.orderId)
            FROM OrderItem oi JOIN Order o ON oi.orderId = o.id
            WHERE oi.productId IN :ids
              AND (:tenantId IS NULL OR oi.tenantId = :tenantId)
              AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            GROUP BY oi.productId
            ORDER BY SUM(oi.unitPrice * oi.quantity) DESC
            """)
    List<Object[]> metricsByProductIds(@Param("ids") List<Long> ids, @Param("tenantId") Long tenantId);

    // "Birlikte sıkça alınanlar": verilen ürünle aynı siparişte geçen diğer ürünler, sıklığa göre.
    @Query("""
            SELECT oi2.productId
            FROM OrderItem oi1 JOIN OrderItem oi2 ON oi1.orderId = oi2.orderId
            WHERE oi1.productId = :productId AND oi2.productId <> :productId
            GROUP BY oi2.productId
            ORDER BY COUNT(oi2.id) DESC
            """)
    List<Long> findFrequentlyBoughtWith(@Param("productId") Long productId, Pageable pageable);

    // Toplam benzersiz sipariş sayısı (per-product COUNT'ları toplamak çift sayar → ayrı sorgu)
    @Query("""
            SELECT COUNT(DISTINCT oi.orderId)
            FROM OrderItem oi JOIN Order o ON oi.orderId = o.id
            WHERE oi.productId IN :ids
              AND (:tenantId IS NULL OR oi.tenantId = :tenantId)
              AND o.status IN (
                com.ecommerce.orderservice.order.constant.OrderStatus.CONFIRMED,
                com.ecommerce.orderservice.order.constant.OrderStatus.SHIPPED,
                com.ecommerce.orderservice.order.constant.OrderStatus.DELIVERED
            )
            """)
    long distinctOrderCountByProductIds(@Param("ids") List<Long> ids, @Param("tenantId") Long tenantId);
}
