package com.ecommerce.productservice.product.controller;

import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.controller.dto.response.AdminProductStatsResponse;
import com.ecommerce.productservice.product.controller.dto.response.AdminProductSummaryResponse;
import com.ecommerce.productservice.product.service.AdminProductService;
import com.ecommerce.productservice.product.service.InternalProductService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
@RequestMapping("/api/v1/products/admin")
@PreAuthorize("hasRole('platform-admin')")
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final InternalProductService internalProductService;
    private final AdminProductService adminProductService;
    private final CacheManager cacheManager;

    // Admin aracı: ES'deki tüm ürünlere tenantName/tenantLogoUrl eklemek için
    // POST /api/v1/products/admin/reindex
    @PostMapping("/reindex")
    public ResponseEntity<String> reindex() {
        int count = internalProductService.reindexAllProducts();
        return ResponseEntity.ok("Reindex tamamlandı. " + count + " ürün için event yazıldı.");
    }

    // Bakım aracı: tüm Redis cache'lerini temizler (public-product, tenant-product, categories...).
    // DB'den elle veri değiştirildikten sonra stale cache'i gidermek için.
    // POST /api/v1/products/admin/clear-cache
    @PostMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        int cleared = 0;
        for (String name : cacheManager.getCacheNames()) {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
                cleared++;
            }
        }
        log.info("[ADMIN] {} cache temizlendi: {}", cleared, cacheManager.getCacheNames());
        return ResponseEntity.ok("Ürün servisi cache'leri temizlendi (" + cleared + " cache).");
    }

    // Overview dashboard: platform geneli ürün sayımları
    // GET /api/v1/products/admin/stats
    @GetMapping("/stats")
    public ResponseEntity<AdminProductStatsResponse> getStats() {
        return ResponseEntity.ok(adminProductService.getStats());
    }

    // Tüm tenant ürün listesi (DELETED hariç) — opsiyonel q/tenantId/categoryId/status filtre
    // GET /api/v1/products/admin/products
    @GetMapping("/products")
    public ResponseEntity<Page<AdminProductSummaryResponse>> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) ProductStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(adminProductService.search(q, tenantId, categoryId, status, pageable));
    }

    // Moderasyon: ürünü satıştan kaldır (INACTIVE)
    // PATCH /api/v1/products/admin/products/{id}/deactivate
    @PatchMapping("/products/{id}/deactivate")
    public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
        adminProductService.deactivateProduct(id);
        return ResponseEntity.noContent().build();
    }

    // Moderasyon: ürünü sil (soft delete)
    // DELETE /api/v1/products/admin/products/{id}
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> removeProduct(@PathVariable Long id) {
        adminProductService.removeProduct(id);
        return ResponseEntity.noContent().build();
    }
}
