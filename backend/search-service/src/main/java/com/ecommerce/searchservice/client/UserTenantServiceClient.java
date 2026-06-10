package com.ecommerce.searchservice.client;

import com.ecommerce.searchservice.client.dto.TenantStorefrontResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-tenant-service", contextId = "searchServiceUtsClient", url = "${application.clients.user-tenant.url}")
public interface UserTenantServiceClient {

    @GetMapping("/api/v1/public/tenants/{tenantId}/storefront")
    TenantStorefrontResponse getTenantStorefront(@PathVariable Long tenantId);
}
