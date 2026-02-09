package com.ecommerce.usertenantservice.common.client;

import com.ecommerce.usertenantservice.tenant.domain.PaymentProcessRequest;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service", url = "${application.config.payment-url}", fallback = PaymentServiceFallback.class)
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/process")
    PaymentResponse processPayment(@RequestBody PaymentProcessRequest request);

}