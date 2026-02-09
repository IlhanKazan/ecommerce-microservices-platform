package com.example.payment_service.subscription.entity;

import com.example.payment_service.common.entity.BaseEntity;
import com.example.payment_service.subscription.constant.BillingCycle;
import com.example.payment_service.subscription.constant.TenantSubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "tenant_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @Column(name = "fee_amount", nullable = false)
    private BigDecimal feeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "cycle_unit")
    private BillingCycle cycleUnit;

    @Column(name = "next_billing_date")
    private LocalDate nextBillingDate;

    @Column(name = "iyzico_card_token")
    private String iyzicoCardToken;

    @Enumerated(EnumType.STRING)
    private TenantSubscriptionStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime lastSuccessfulPaymentDate;
    private Integer failedPaymentCount;

    @Column(name = "auto_renew")
    private boolean autoRenew;

    private String cancellationReason;

}
