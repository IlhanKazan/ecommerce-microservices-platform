package com.ecommerce.orderservice.order.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.orderservice.order.constant.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "shipping_address_json", nullable = false, columnDefinition = "TEXT")
    private String shippingAddressJson;

    @Column(name = "payment_transaction_id")
    private String paymentTransactionId;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "buyer_email", length = 320)
    private String buyerEmail;

    @Builder
    public Order(UUID userId, Long tenantId, BigDecimal totalAmount,
                 String currency, String shippingAddressJson, String paymentTransactionId,
                 BigDecimal commissionAmount, String buyerEmail) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.status = OrderStatus.CONFIRMED;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.shippingAddressJson = shippingAddressJson;
        this.paymentTransactionId = paymentTransactionId;
        this.commissionAmount = commissionAmount;
        this.buyerEmail = buyerEmail;
    }

    public void ship() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new BusinessException("Sadece onaylanmış siparişler kargoya verilebilir!", "INVALID_ORDER_STATUS");
        }
        this.status = OrderStatus.SHIPPED;
    }

    public void deliver() {
        if (this.status != OrderStatus.SHIPPED) {
            throw new BusinessException("Sadece kargodaki siparişler teslim edilmiş olarak işaretlenebilir!", "INVALID_ORDER_STATUS");
        }
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now(); // 14 günlük iade penceresi buradan sayılır
    }

    public void cancel(String reason) {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new BusinessException("Sadece onaylanmış siparişler iptal edilebilir!", "INVALID_ORDER_STATUS");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public void refund(String reason) {
        if (this.status != OrderStatus.CANCELLED) {
            throw new BusinessException("İade yalnızca iptal edilmiş siparişlere uygulanabilir!", "INVALID_ORDER_STATUS");
        }
        this.status = OrderStatus.REFUNDED;
        this.cancellationReason = reason;
    }

    // ─── İade (return) yaşam döngüsü — detay OrderReturn entity'sinde ───
    public void requestReturn() {
        if (this.status != OrderStatus.DELIVERED) {
            throw new BusinessException("Sadece teslim edilmiş siparişler için iade talebi açılabilir!", "INVALID_ORDER_STATUS");
        }
        this.status = OrderStatus.RETURN_REQUESTED;
    }

    public void approveReturn() {
        if (this.status != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException("Sadece iade talebi açık siparişler onaylanabilir!", "INVALID_ORDER_STATUS");
        }
        this.status = OrderStatus.RETURNED;
    }

    public void rejectReturn() {
        if (this.status != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException("Sadece iade talebi açık siparişler reddedilebilir!", "INVALID_ORDER_STATUS");
        }
        // Terminal: reddedilen sipariş tekrar iade talebi açamaz (DELIVERED'a DÖNMEZ)
        this.status = OrderStatus.RETURN_REJECTED;
    }
}
