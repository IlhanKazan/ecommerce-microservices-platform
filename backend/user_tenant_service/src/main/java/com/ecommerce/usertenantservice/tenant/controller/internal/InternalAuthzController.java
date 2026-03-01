package com.ecommerce.usertenantservice.tenant.controller.internal;

import com.ecommerce.usertenantservice.tenant.service.AuthzCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/authz")
@RequiredArgsConstructor
public class InternalAuthzController {

    private final AuthzCacheService authzCacheService;

    @GetMapping("/users/{userId}/tenants/{tenantId}/role")
    public ResponseEntity<String> getUserRole(@PathVariable UUID userId, @PathVariable Long tenantId) {
        String role = authzCacheService.fetchAndCacheUserRole(userId, tenantId);
        return ResponseEntity.ok(role);
    }
}