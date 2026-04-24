package com.ecommerce.paymentservice.subscription.controller.dto.response;

import com.ecommerce.paymentservice.subscription.constant.BillingCycle;
import java.math.BigDecimal;

public record SubscriptionPlanResponse(
        Long id,
        String name,
        BigDecimal price,
        String currency,
        BillingCycle billingCycle,
        String features
) {
}
