package com.example.payment_service.subscription.service.impl;

import com.example.payment_service.subscription.entity.SubscriptionPlan;
import com.example.payment_service.subscription.repository.SubscriptionPlanRepository;
import com.example.payment_service.subscription.service.SubscriptionPlanService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionPlanServiceImpl implements SubscriptionPlanService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlanServiceImpl(SubscriptionPlanRepository subscriptionPlanRepository){
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Override
    public Optional<SubscriptionPlan> findByIdAndIsActive(Long planId, boolean isActive){
        return subscriptionPlanRepository.findByIdAndIsActive(planId, isActive);
    }

    @Override
    public SubscriptionPlan save(SubscriptionPlan subscriptionPlan){
        return subscriptionPlanRepository.save(subscriptionPlan);
    }

    @Override
    public List<SubscriptionPlan> findAllByIsActive(boolean isActive) {
        return subscriptionPlanRepository.findAllByIsActive(isActive);
    }

}
