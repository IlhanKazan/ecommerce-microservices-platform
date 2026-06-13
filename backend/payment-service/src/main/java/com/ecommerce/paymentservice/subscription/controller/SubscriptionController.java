package com.ecommerce.paymentservice.subscription.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.paymentservice.common.constants.ApiPaths;
import com.ecommerce.paymentservice.common.security.TenantOwnershipGuard;
import com.ecommerce.paymentservice.subscription.controller.dto.request.ChangePlanRequest;
import com.ecommerce.paymentservice.subscription.controller.dto.response.ChangePlanResponse;
import com.ecommerce.paymentservice.subscription.controller.dto.response.SubscriptionPlanResponse;
import com.ecommerce.paymentservice.subscription.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.paymentservice.subscription.domain.ChangePlanResult;
import com.ecommerce.paymentservice.subscription.entity.SubscriptionPlan;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.ecommerce.paymentservice.subscription.mapper.SubscriptionPlanMapper;
import com.ecommerce.paymentservice.subscription.mapper.TenantSubscriptionMapper;
import com.ecommerce.paymentservice.subscription.service.ChangePlanService;
import com.ecommerce.paymentservice.subscription.service.SubscriptionPlanService;
import com.ecommerce.paymentservice.subscription.service.TenantSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Subscription.SUBSCRIPTION)
@Tag(name = "Subscriptions", description = "Subscription plan catalog and active tenant subscription management")
public class SubscriptionController {

    private final SubscriptionPlanService subscriptionPlanService;
    private final SubscriptionPlanMapper subscriptionPlanMapper;
    private final TenantSubscriptionService tenantSubscriptionService;
    private final TenantSubscriptionMapper tenantSubscriptionMapper;
    private final ChangePlanService changePlanService;
    private final TenantOwnershipGuard ownershipGuard;

    public SubscriptionController(SubscriptionPlanService subscriptionPlanService, SubscriptionPlanMapper subscriptionPlanMapper, TenantSubscriptionService tenantSubscriptionService, TenantSubscriptionMapper tenantSubscriptionMapper, ChangePlanService changePlanService, TenantOwnershipGuard ownershipGuard) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.subscriptionPlanMapper = subscriptionPlanMapper;
        this.tenantSubscriptionService = tenantSubscriptionService;
        this.tenantSubscriptionMapper = tenantSubscriptionMapper;
        this.changePlanService = changePlanService;
        this.ownershipGuard = ownershipGuard;
    }

    @Operation(summary = "List subscription plans", description = "Returns all active subscription plans with pricing, features and commission rates.", security = {})
    @ApiResponse(responseCode = "200", description = "List of active plans")
    @GetMapping("/plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> findAllByIsActive() {
        List<SubscriptionPlan> activePlans = subscriptionPlanService.findAllByIsActive(true);
        List<SubscriptionPlanResponse> response = subscriptionPlanMapper.toResponseList(activePlans);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get tenant subscription", description = "Returns the latest active subscription for a tenant including billing dates and commission rate.")
    @ApiResponse(responseCode = "200", description = "Subscription detail")
    @ApiResponse(responseCode = "404", description = "No active subscription found")
    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantSubscriptionResponse> getSubscriptionDetails(@PathVariable Long tenantId){
        TenantSubscription tenantSubscription = tenantSubscriptionService.getSubscriptionDetails(tenantId);
        TenantSubscriptionResponse response = tenantSubscriptionMapper.toResponse(tenantSubscription);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change subscription plan",
            description = "Upgrade: kalan güne göre prorate fark anında varsayılan karttan tahsil edilir, billing tarihi sabit. "
                    + "Downgrade: döngü sonunda uygulanır (tahsilat yok). Sadece mağaza sahibi çağırabilir.")
    @ApiResponse(responseCode = "200", description = "Plan değişikliği uygulandı/planlandı")
    @ApiResponse(responseCode = "400", description = "Geçersiz değişiklik (aynı plan / ödeme başarısız)")
    @ApiResponse(responseCode = "403", description = "Mağaza sahibi değil")
    @ApiResponse(responseCode = "409", description = "Aktif abonelik yok / varsayılan kart yok")
    @PostMapping("/change-plan")
    @Idempotent
    public ResponseEntity<ChangePlanResponse> changePlan(@RequestBody ChangePlanRequest request, @CurrentUser AuthUser user) {
        ownershipGuard.requireOwner(user, request.tenantId());
        ChangePlanResult result = changePlanService.changePlan(request.tenantId(), request.planId());
        ChangePlanResponse response = new ChangePlanResponse(
                result.changeType().name(),
                result.chargedAmount(),
                result.effectiveDate(),
                result.newPlanName()
        );
        return ResponseEntity.ok(response);
    }

}
