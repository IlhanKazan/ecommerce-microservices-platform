package com.ecommerce.paymentservice.outbox.service;

import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface OutboxService {
    void publishPaymentSuccessEvent(Payment payment);
    void publishPaymentFailedEvent(Payment payment);
    void publishSubscriptionActivatedEvent(TenantSubscription subscription);
    void publishSubscriptionRenewalSuccessEvent(TenantSubscription subscription);
    void publishSubscriptionRenewalFailedEvent(TenantSubscription subscription, String failureReason, boolean suspended);
    void publishSubscriptionPlanChangedEvent(TenantSubscription subscription, String oldPlanName, String newPlanName,
                                             String changeType, LocalDate effectiveDate, BigDecimal chargedAmount);
}
