package com.ecommerce.usertenantservice.tenant.controller.dto.request;

import com.ecommerce.usertenantservice.tenant.domain.PaymentCardInfo;

public record RetrySubscriptionRequest(
        Long planId,
        PaymentCardInfo newCardInfo
) {}
