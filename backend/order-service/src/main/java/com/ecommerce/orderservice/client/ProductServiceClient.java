package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ProductSnapshotInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", url = "${application.clients.product.url}")
public interface ProductServiceClient {

    @GetMapping("/api/v1/internal/products/{productId}/tenants/{tenantId}/validate")
    ProductSnapshotInfo getProductSnapshot(
            @PathVariable("productId") Long productId,
            @PathVariable("tenantId") Long tenantId
    );
}
