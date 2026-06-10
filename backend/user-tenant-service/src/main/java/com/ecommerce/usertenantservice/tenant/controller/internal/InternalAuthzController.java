package com.ecommerce.usertenantservice.tenant.controller.internal;

import com.ecommerce.usertenantservice.tenant.repository.TenantRepository;
import com.ecommerce.usertenantservice.tenant.service.AuthzCacheService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Hidden
@RestController
@RequestMapping("/api/v1/internal/authz")
@RequiredArgsConstructor
public class InternalAuthzController {

    private final AuthzCacheService authzCacheService;
    private final TenantRepository tenantRepository;

    @GetMapping("/users/{userId}/tenants/{tenantId}/role")
    public ResponseEntity<String> getUserRole(@PathVariable UUID userId, @PathVariable Long tenantId) {
        String role = authzCacheService.fetchAndCacheUserRole(userId, tenantId);
        return ResponseEntity.ok(role);
    }

    @GetMapping("/tenants/{tenantId}/submerchant-key")
    public ResponseEntity<String> getSubMerchantKey(@PathVariable Long tenantId) {
        String key = tenantRepository.findById(tenantId)
                .map(t -> t.getIyzicoSubMerchantKey())
                .orElse(null);
        return ResponseEntity.ok(key);
    }
}