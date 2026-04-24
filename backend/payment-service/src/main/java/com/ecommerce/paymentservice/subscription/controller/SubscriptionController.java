package com.ecommerce.paymentservice.subscription.controller;

import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.subscription.controller.dto.response.SubscriptionPlanResponse;
import com.ecommerce.paymentservice.subscription.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.paymentservice.subscription.entity.SubscriptionPlan;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.mapper.SubscriptionPlanMapper;
import com.ecommerce.paymentservice.subscription.mapper.TenantSubscriptionMapper;
import com.ecommerce.paymentservice.subscription.service.SubscriptionPlanService;
import com.ecommerce.paymentservice.subscription.service.TenantSubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
