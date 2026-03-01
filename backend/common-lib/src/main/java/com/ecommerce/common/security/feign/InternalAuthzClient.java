package com.ecommerce.common.security.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// TODO [01.03.2026 00:27]: urlyi Hardcodelamak zorunda kaldık
@FeignClient(name = "user-tenant-service", url = "http://localhost:8081", path = "/api/v1/internal/authz")
public interface InternalAuthzClient {

    @GetMapping("/users/{userId}/tenants/{tenantId}/role")
    String getUserRoleInTenant(@PathVariable("userId") String userId, @PathVariable("tenantId") Long tenantId);
}