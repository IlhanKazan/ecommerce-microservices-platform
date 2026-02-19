package com.example.payment_service.subscription.controller;

import com.example.payment_service.common.constants.ApiPaths;
import com.example.payment_service.subscription.controller.dto.response.SubscriptionPlanResponse;
import com.example.payment_service.subscription.controller.dto.response.TenantSubscriptionResponse;
import com.example.payment_service.subscription.entity.SubscriptionPlan;
import com.example.payment_service.subscription.entity.TenantSubscription;
import com.example.payment_service.subscription.mapper.SubscriptionPlanMapper;
import com.example.payment_service.subscription.mapper.TenantSubscriptionMapper;
import com.example.payment_service.subscription.service.SubscriptionPlanService;
import com.example.payment_service.subscription.service.TenantSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(ApiPaths.Subscription.SUBSCRIPTION)
public class SubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPlanMapper subscriptionPlanMapper;
    private final TenantSubscriptionService tenantSubscriptionService;
    private final TenantSubscriptionMapper tenantSubscriptionMapper;

    public SubscriptionController(SubscriptionPlanService subscriptionPlanService, SubscriptionPlanMapper subscriptionPlanMapper, TenantSubscriptionService tenantSubscriptionService, TenantSubscriptionMapper tenantSubscriptionMapper) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.subscriptionPlanMapper = subscriptionPlanMapper;
        this.tenantSubscriptionService = tenantSubscriptionService;
        this.tenantSubscriptionMapper = tenantSubscriptionMapper;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> findAllByIsActive() {
        List<SubscriptionPlan> activePlans = subscriptionPlanService.findAllByIsActive(true);
        List<SubscriptionPlanResponse> response = subscriptionPlanMapper.toResponseList(activePlans);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantSubscriptionResponse> getSubscriptionDetails(@PathVariable Long tenantId){
        TenantSubscription tenantSubscription = tenantSubscriptionService.getSubscriptionDetails(tenantId);
        TenantSubscriptionResponse response = tenantSubscriptionMapper.toResponse(tenantSubscription);
        return ResponseEntity.ok(response);
    }

}
