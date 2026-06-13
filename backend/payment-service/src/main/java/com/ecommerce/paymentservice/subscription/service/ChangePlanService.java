package com.ecommerce.paymentservice.subscription.service;

import com.ecommerce.paymentservice.subscription.domain.ChangePlanResult;

public interface ChangePlanService {

    /**
     * Tenant'ın aktif aboneliğini yeni plana geçirir.
     * Upgrade: kalan güne göre prorate fark anında varsayılan karttan tahsil edilir, billing tarihi sabit.
     * Downgrade: döngü sonunda uygulanmak üzere planlanır (scheduledPlanId).
     */
    ChangePlanResult changePlan(Long tenantId, Long newPlanId);
}
