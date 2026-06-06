package com.ecommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-tenant-service", contextId = "userTenantSubMerchantClient", url = "${application.clients.user-tenant.url}")
public interface UserTenantServiceClient {

    @GetMapping("/api/v1/internal/authz/tenants/{tenantId}/submerchant-key")
    String getSubMerchantKey(@PathVariable Long tenantId);
}
