package com.ecommerce.paymentservice.subscription.service;

import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;

import java.math.BigDecimal;
import java.util.Optional;

public interface TenantSubscriptionService {
    TenantSubscription save(TenantSubscription tenantSubscription);
    TenantSubscription createActiveSubscription(Long tenantId, Long planId, String cardToken, BigDecimal amountPaid);
    TenantSubscription getSubscriptionDetails(Long tenantId);
    Optional<TenantSubscription> findLatestSubscription(Long tenantId);
}
