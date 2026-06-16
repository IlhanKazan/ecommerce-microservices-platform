package com.ecommerce.usertenantservice.tenant.controller;

import com.ecommerce.usertenantservice.common.constants.ApiPaths;
import com.ecommerce.usertenantservice.tenant.constant.TenantStatus;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantResponse;
import com.ecommerce.usertenantservice.tenant.controller.dto.response.TenantSummaryResponse;
import com.ecommerce.usertenantservice.tenant.service.AdminTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.Tenant.ADMIN_TENANT)
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('platform-admin')")
@Tag(name = "Admin — Stores", description = "Platform admin store management — list, suspend, reactivate (requires platform-admin role)")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;
    // UTS'de Spring CacheManager bean'i her ortamda olmayabilir → opsiyonel (startup'ı kırmasın).
    private final ObjectProvider<CacheManager> cacheManagerProvider;

    @Operation(summary = "List all stores", description = "Paginated list of all tenant stores. Optional status + name/business-name search filters.")
    @ApiResponse(responseCode = "200", description = "Store page")
    @ApiResponse(responseCode = "403", description = "Not a platform admin")
    @GetMapping("/stores")
    public ResponseEntity<Page<TenantSummaryResponse>> listStores(
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(adminTenantService.listStores(status, verified, q, pageable));
    }

    @Operation(summary = "Get store detail", description = "Full tenant profile for the admin store detail view.")
    @ApiResponse(responseCode = "200", description = "Store detail")
    @ApiResponse(responseCode = "404", description = "Store not found")
    @GetMapping("/stores/{tenantId}")
    public ResponseEntity<TenantResponse> getStoreDetail(@PathVariable Long tenantId) {
        return ResponseEntity.ok(adminTenantService.getStoreDetail(tenantId));
    }

    @Operation(summary = "Suspend store", description = "Suspends a store (ACTIVE/PASSIVE → SUSPENDED). Products are pulled from sale/search; members keep dashboard access. Reversible via reactivate.")
    @ApiResponse(responseCode = "200", description = "Store suspended")
    @ApiResponse(responseCode = "400", description = "Store is closed or already suspended")
    @PostMapping("/stores/{tenantId}/suspend")
    public ResponseEntity<Void> suspendStore(@PathVariable Long tenantId) {
        adminTenantService.suspend(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reactivate store", description = "Reactivates a suspended store (SUSPENDED → ACTIVE). Products return to sale/search.")
    @ApiResponse(responseCode = "200", description = "Store reactivated")
    @ApiResponse(responseCode = "400", description = "Store is not suspended")
    @PostMapping("/stores/{tenantId}/reactivate")
    public ResponseEntity<Void> reactivateStore(@PathVariable Long tenantId) {
        adminTenantService.reactivate(tenantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Clear caches", description = "Tüm Redis cache'lerini temizler (storefront/user vb.). DB'den elle veri değiştirildikten sonra stale cache'i gidermek için.")
    @ApiResponse(responseCode = "200", description = "Cache temizlendi")
    @PostMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            return ResponseEntity.ok("Mağaza servisinde aktif cache yok.");
        }
        int cleared = 0;
        for (String name : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                cleared++;
            }
        }
        log.info("[ADMIN] {} cache temizlendi: {}", cleared, cacheManager.getCacheNames());
        return ResponseEntity.ok("Mağaza servisi cache'leri temizlendi (" + cleared + " cache).");
    }
}
