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

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "buyer_email", length = 320)
    private String buyerEmail;

    @Builder
    public Order(UUID userId, Long tenantId, BigDecimal totalAmount,
                 String currency, String shippingAddressJson, String paymentTransactionId,
                 String buyerEmail) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.status = OrderStatus.CONFIRMED;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.shippingAddressJson = shippingAddressJson;
        this.paymentTransactionId = paymentTransactionId;
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
}
