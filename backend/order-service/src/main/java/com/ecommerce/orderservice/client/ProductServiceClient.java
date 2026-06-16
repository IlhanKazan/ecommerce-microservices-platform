package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.ProductSnapshotInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", url = "${application.clients.product.url}")
public interface ProductServiceClient {

    @GetMapping("/api/v1/internal/products/{productId}/tenants/{tenantId}/validate")
    ProductSnapshotInfo getProductSnapshot(
            @PathVariable("productId") Long productId,
            @PathVariable("tenantId") Long tenantId
    );

    // Satış metriği: ürünün kendisi + tüm varyant id'leri (parent → tüm child'lar)
    @GetMapping("/api/v1/internal/products/{productId}/variant-ids")
    List<Long> getSalesIds(@PathVariable("productId") Long productId);
}
