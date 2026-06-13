package com.ecommerce.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * user-tenant-service authz endpoint'i — kullanıcının bir tenant üzerindeki rolünü döndürür.
 * Üye değilse "NONE" döner. Kart/plan gibi para işlemlerinde sahiplik doğrulaması için kullanılır.
 */
@FeignClient(name = "user-tenant-service", contextId = "paymentAuthzClient", url = "${application.clients.user-tenant.url}")
public interface UserTenantAuthzClient {

    @GetMapping("/api/v1/internal/authz/users/{userId}/tenants/{tenantId}/role")
    String getUserRole(@PathVariable UUID userId, @PathVariable Long tenantId);
}
