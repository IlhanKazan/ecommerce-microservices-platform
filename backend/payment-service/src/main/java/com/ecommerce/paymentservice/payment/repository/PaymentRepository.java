package com.ecommerce.paymentservice.payment.repository;

import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Payment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
    Optional<Payment> findByOrderId(Long orderId);

    // İade lookup'ı: PRODUCT_ORDER ödemelerinde Payment.orderId NULL (ödeme sipariş persist'inden önce yapılır),
    // ödeme iyzico paymentId ile (Order.paymentTransactionId = Payment.iyzicoTransactionId) tanımlanır.
    Optional<Payment> findByIyzicoTransactionId(String iyzicoTransactionId);

    // --- Platform admin: gelir özeti ---
    // Tip bazında: [paymentType, SUM(amount), COALESCE(SUM(commissionAmount),0), COUNT]
    @Query("""
            SELECT p.paymentType, SUM(p.amount), COALESCE(SUM(p.commissionAmount), 0), COUNT(p)
            FROM Payment p
            WHERE p.paymentStatus = :status
            GROUP BY p.paymentType
            """)
    List<Object[]> revenueByType(@Param("status") PaymentStatus status);

    // Aylık platform geliri serisi (grafik): [YYYY-MM, abonelik amount toplamı, PRODUCT_ORDER komisyon toplamı]
    @Query("""
            SELECT FUNCTION('to_char', p.paidAt, 'YYYY-MM'),
                   SUM(CASE WHEN p.paymentType = com.ecommerce.paymentservice.payment.constant.PaymentType.SUBSCRIPTION THEN p.amount ELSE 0 END),
                   SUM(CASE WHEN p.paymentType = com.ecommerce.paymentservice.payment.constant.PaymentType.PRODUCT_ORDER THEN COALESCE(p.commissionAmount, 0) ELSE 0 END)
            FROM Payment p
            WHERE p.paymentStatus = :status AND p.paidAt IS NOT NULL
            GROUP BY FUNCTION('to_char', p.paidAt, 'YYYY-MM')
            ORDER BY FUNCTION('to_char', p.paidAt, 'YYYY-MM')
            """)
    List<Object[]> monthlyRevenue(@Param("status") PaymentStatus status);

    // --- Platform admin: ödeme listesi (transactions) ---
    // Enum param'lar null-safe; bytea sorunu (string :q) yok.
    @Query("""
            SELECT p FROM Payment p
            WHERE (:type IS NULL OR p.paymentType = :type)
              AND (:status IS NULL OR p.paymentStatus = :status)
              AND (:tenantId IS NULL OR p.tenantId = :tenantId)
            ORDER BY p.createdAt DESC
            """)
    Page<Payment> searchForAdmin(@Param("type") PaymentType type,
                                 @Param("status") PaymentStatus status,
                                 @Param("tenantId") Long tenantId,
                                 Pageable pageable);
}
