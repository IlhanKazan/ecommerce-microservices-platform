package com.ecommerce.paymentservice.payment.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.paymentservice.payment.constant.PaymentMethod;
import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import java.time.LocalDateTime;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    private BigDecimal refundedAmount;
    private BigDecimal netAmount;

    @Column(name = "commission_amount")
    private BigDecimal commissionAmount;

    @Column(name = "commission_rate")
    private BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private String failureReason;
    private String failureCode;
}
