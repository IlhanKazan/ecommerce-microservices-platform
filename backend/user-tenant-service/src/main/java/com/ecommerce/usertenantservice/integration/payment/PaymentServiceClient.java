package com.ecommerce.usertenantservice.integration.payment;

import com.ecommerce.common.interceptor.FeignClientInterceptor;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantCreateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.request.SubMerchantUpdateRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.SubMerchantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.usertenantservice.tenant.command.PaymentProcessRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "payment-service", url = "${application.config.payment-url}", configuration = FeignClientInterceptor.class)
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/process")
    PaymentResponse processPayment(@RequestBody PaymentProcessRequest request);

    @GetMapping("/api/v1/subscriptions/tenants/{tenantId}")
    TenantSubscriptionResponse getSubscriptionDetails(@PathVariable Long tenantId);

    @GetMapping("/api/v1/payments/history/users/{userId}")
    Page<PaymentHistoryResponse> getUserPaymentHistory(@PathVariable Long userId, Pageable pageable);

    @GetMapping("/api/v1/payments/history/tenants/{tenantId}")
    Page<PaymentHistoryResponse> getTenantPaymentHistory(@PathVariable Long tenantId, Pageable pageable);

    @PostMapping("/api/v1/submerchant/create")
    SubMerchantResponse createSubMerchant(@RequestBody SubMerchantCreateRequest request);

    @PutMapping("/api/v1/submerchant/update")
    SubMerchantResponse updateSubMerchant(@RequestBody SubMerchantUpdateRequest request);

}