package com.ecommerce.stockservice.client;

import com.ecommerce.stockservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${application.clients.product.url}")
public interface ProductClient {

    @GetMapping("/api/v1/products/tenants/{tenantId}/{productId}")
    ProductResponse getTenantProduct(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("productId") Long productId
    );

}