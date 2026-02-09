package com.example.payment_service.subscription.controller;

import com.example.payment_service.common.constants.ApiPaths;
import com.example.payment_service.subscription.controller.dto.response.SubscriptionPlanResponse;
import com.example.payment_service.subscription.entity.SubscriptionPlan;
import com.example.payment_service.subscription.mapper.SubscriptionPlanMapper;
import com.example.payment_service.subscription.service.SubscriptionPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Subscription.SUBSCRIPTION)
public class SubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPlanMapper subscriptionPlanMapper;

    public SubscriptionController(SubscriptionPlanService subscriptionPlanService, SubscriptionPlanMapper subscriptionPlanMapper) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.subscriptionPlanMapper = subscriptionPlanMapper;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> findAllByIsActive() {
        List<SubscriptionPlan> activePlans = subscriptionPlanService.findAllByIsActive(true);
        List<SubscriptionPlanResponse> response = subscriptionPlanMapper.toResponseList(activePlans);
        return ResponseEntity.ok(response);
    }
}
