package com.ecommerce.usertenantservice.integration.payment;

import com.ecommerce.usertenantservice.integration.config.FeignClientInterceptor;
import com.ecommerce.usertenantservice.integration.payment.config.PaymentServiceFallback;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentHistoryResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSubscriptionResponse;
import com.ecommerce.usertenantservice.tenant.domain.PaymentProcessRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${application.config.payment-url}", configuration = FeignClientInterceptor.class, fallback = PaymentServiceFallback.class)
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/process")
    PaymentResponse processPayment(@RequestBody PaymentProcessRequest request);

    @GetMapping("/api/v1/subscriptions/tenants/{tenantId}")
    TenantSubscriptionResponse getSubscriptionDetails(@PathVariable Long tenantId);

    @GetMapping("/api/v1/payments/history/users/{userId}")
    Page<PaymentHistoryResponse> getUserPaymentHistory(@PathVariable Long userId, Pageable pageable);

    @GetMapping("/api/v1/payments/history/tenants/{tenantId}")
    Page<PaymentHistoryResponse> getTenantPaymentHistory(@PathVariable Long tenantId, Pageable pageable);

}