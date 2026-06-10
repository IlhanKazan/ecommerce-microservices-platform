package com.ecommerce.productservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "order-service", contextId = "orderServiceClient", url = "${application.clients.order-service.url}")
public interface OrderServiceClient {

    @GetMapping("/api/v1/internal/orders/has-purchased")
    Boolean hasPurchased(@RequestParam UUID userId, @RequestParam Long productId);
}
