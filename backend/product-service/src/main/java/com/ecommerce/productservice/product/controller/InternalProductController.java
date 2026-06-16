package com.ecommerce.productservice.product.controller;

import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.product.controller.dto.request.AiReportUpdateRequest;
import com.ecommerce.productservice.product.query.ProductValidationInfo;
import com.ecommerce.productservice.product.query.StockGroupInfo;
import com.ecommerce.productservice.product.service.InternalProductService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Hidden
@RestController
@RequestMapping(ApiPaths.Internal.INTERNAL_PRODUCTS)
@RequiredArgsConstructor
@Slf4j
public class InternalProductController {

    private final InternalProductService internalProductService;

    // Stock-service ve order-service bu endpoint'i çağırır
    // GET /api/v1/internal/products/{productId}/tenants/{tenantId}/validate
    @GetMapping("/{productId}/tenants/{tenantId}/validate")
    public ResponseEntity<ProductValidationInfo> validateProduct(
            @PathVariable Long productId,
            @PathVariable Long tenantId) {

        ProductValidationInfo info =
                internalProductService.validateAndGetProduct(productId, tenantId);
        return ResponseEntity.ok(info);
    }

    // AI Engine callback: ürüne AI yorumlar raporu yaz
    // PATCH /api/v1/internal/products/{productId}/ai-report
    @PatchMapping("/{productId}/ai-report")
    public ResponseEntity<Void> updateAiReport(
            @PathVariable Long productId,
            @RequestBody AiReportUpdateRequest request) {
        internalProductService.updateAiReport(productId, request.aiReviewReport());
        return ResponseEntity.ok().build();
    }

    // Order-service satış metriği: ürünün kendisi + tüm varyant id'leri
    // GET /api/v1/internal/products/{productId}/variant-ids
    @GetMapping("/{productId}/variant-ids")
    public ResponseEntity<java.util.List<Long>> getSalesAggregationIds(@PathVariable Long productId) {
        return ResponseEntity.ok(internalProductService.getSalesAggregationIds(productId));
    }

    // Stock-service ES stok agregasyonu: stoğu değişen ürünün search-index hedefi + üye id'leri
    // GET /api/v1/internal/products/{productId}/stock-group
    @GetMapping("/{productId}/stock-group")
    public ResponseEntity<StockGroupInfo> resolveStockGroup(@PathVariable Long productId) {
        return ResponseEntity.ok(internalProductService.resolveStockGroup(productId));
    }

}