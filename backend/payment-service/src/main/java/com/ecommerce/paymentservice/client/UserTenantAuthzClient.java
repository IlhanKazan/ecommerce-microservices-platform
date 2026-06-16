package com.ecommerce.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * user-tenant-service internal endpoint'leri.
 * - authz: kullanıcının bir tenant üzerindeki rolü (üye değilse "NONE").
 * - tenant adları: admin transaction listesinde "kimden" gösterimi için id→ad eşlemesi.
 */
@FeignClient(name = "user-tenant-service", contextId = "paymentAuthzClient", url = "${application.clients.user-tenant.url}")
public interface UserTenantAuthzClient {

    @GetMapping("/api/v1/internal/authz/users/{userId}/tenants/{tenantId}/role")
    String getUserRole(@PathVariable UUID userId, @PathVariable Long tenantId);

    @GetMapping("/api/v1/internal/tenants/names")
    Map<Long, String> getTenantNames(@RequestParam("ids") List<Long> ids);
}
