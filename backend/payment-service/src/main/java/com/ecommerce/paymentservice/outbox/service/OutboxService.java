package com.ecommerce.paymentservice.outbox.service;

import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;

public interface OutboxService {
    void publishPaymentSuccessEvent(Payment payment);
    void publishPaymentFailedEvent(Payment payment);
    void publishSubscriptionActivatedEvent(TenantSubscription subscription);
}
