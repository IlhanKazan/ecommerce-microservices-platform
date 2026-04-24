package com.ecommerce.common.security.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-tenant-service", url = "${application.clients.user-tenant.url}", path = "/api/v1/internal/authz")
public interface InternalAuthzClient {

    @GetMapping("/users/{userId}/tenants/{tenantId}/role")
    String getUserRoleInTenant(@PathVariable("userId") String userId, @PathVariable("tenantId") Long tenantId);
}