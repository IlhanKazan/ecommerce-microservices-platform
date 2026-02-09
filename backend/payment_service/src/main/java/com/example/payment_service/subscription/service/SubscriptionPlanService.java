package com.example.payment_service.subscription.service;

import com.example.payment_service.subscription.entity.SubscriptionPlan;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanService {
    Optional<SubscriptionPlan> findByIdAndIsActive(Long planId, boolean isActive);
    SubscriptionPlan save(SubscriptionPlan subscriptionPlan);
    List<SubscriptionPlan> findAllByIsActive(boolean isActive);
}
