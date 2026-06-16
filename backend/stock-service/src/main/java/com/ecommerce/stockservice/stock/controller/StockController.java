package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.stock.controller.dto.request.AddStockRequest;
import com.ecommerce.stockservice.stock.controller.dto.request.BatchAddStockRequest;
import com.ecommerce.stockservice.stock.controller.dto.request.UpdateThresholdRequest;
import com.ecommerce.stockservice.stock.controller.dto.response.StockResponse;
import com.ecommerce.stockservice.stock.controller.dto.response.StockSummaryResponse;
import com.ecommerce.stockservice.stock.query.StockInfo;
import com.ecommerce.stockservice.stock.query.StockSummaryInfo;
import com.ecommerce.stockservice.stock.service.StockService;
import com.ecommerce.stockservice.stock.entity.Stock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPaths.Stocks.STOCKS_PATH)
@RequiredArgsConstructor
@Tag(name = "Stock", description = "Inventory management — manual stock add/remove, warehouse stock status per tenant")
public class StockController {

    private final StockService stockService;

    @Operation(summary = "Add stock manually", description = "Manually adds inventory to a warehouse for a product. Triggers STOCK_STATUS_CHANGED event if product becomes available.")
    @ApiResponse(responseCode = "200", description = "Stock added — returns success message")
    @ApiResponse(responseCode = "403", description = "Not authorized for this tenant")
    @ApiResponse(responseCode = "404", description = "Product or warehouse not found")
    @Idempotent(cachePrefix = "idempotency:manual-add:")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    @PostMapping(ApiPaths.Stocks.TENANT_STOCKS_PATH + "/manual-add")
    public ResponseEntity<String> addManualStock(
            @PathVariable Long tenantId,
            @CurrentUser AuthUser user,
            @Valid @RequestBody AddStockRequest request) {

        stockService.addManualStock(
                tenantId,
                request.warehouseId(),
                request.productId(),
                request.amount(),
                user.keycloakId()
        );

        return ResponseEntity.ok("Stok başarıyla eklendi.");
    }

    @Operation(summary = "Add stock in batch", description = "Tek depoya birden çok ürün/varyant için toplu stok girişi. Varyant matris üretici akışında, oluşturulan varyantlara ilk stoğu tek çağrıyla tohumlamak için. Atomik (biri patlarsa hepsi geri alınır).")
    @ApiResponse(responseCode = "200", description = "Toplu stok eklendi")
    @ApiResponse(responseCode = "403", description = "Not authorized for this tenant")
    @ApiResponse(responseCode = "404", description = "Depo veya ürün bulunamadı")
    @Idempotent(cachePrefix = "idempotency:manual-add-batch:")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    @PostMapping(ApiPaths.Stocks.TENANT_STOCKS_PATH + "/manual-add/batch")
    public ResponseEntity<String> addManualStockBatch(
            @PathVariable Long tenantId,
            @CurrentUser AuthUser user,
            @Valid @RequestBody BatchAddStockRequest request) {

        // Controller: DTO → primitive map. Service DTO kabul etmez.
        Map<Long, Integer> productAmounts = request.items().stream()
                .collect(java.util.stream.Collectors.toMap(
                        BatchAddStockRequest.Item::productId,
                        BatchAddStockRequest.Item::amount,
                        Integer::sum));

        stockService.addManualStockBatch(tenantId, request.warehouseId(), productAmounts, user.keycloakId());

        return ResponseEntity.ok(productAmounts.size() + " ürün için stok eklendi.");
    }

    @Operation(summary = "Remove stock manually", description = "Manually removes inventory from a warehouse. Triggers STOCK_STATUS_CHANGED event if product goes out of stock.")
    @ApiResponse(responseCode = "200", description = "Stock removed")
    @ApiResponse(responseCode = "422", description = "Insufficient stock to remove")
    @Idempotent(cachePrefix = "idempotency:manual-remove:")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    @PostMapping(ApiPaths.Stocks.TENANT_STOCKS_PATH + "/manual-remove")
    public ResponseEntity<String> removeManualStock(
            @PathVariable Long tenantId,
            @CurrentUser AuthUser user,
            @Valid @RequestBody AddStockRequest request) {

        stockService.removeManualStock(
                tenantId,
                request.warehouseId(),
                request.productId(),
                request.amount(),
                user.keycloakId()
        );

        return ResponseEntity.ok("Stok başarıyla düşüldü.");
    }

    @Operation(summary = "List tenant stock", description = "Returns stock levels for all products in this tenant across all warehouses.")
    @ApiResponse(responseCode = "200", description = "Stock summary list")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    @GetMapping(ApiPaths.Stocks.TENANT_STOCKS_PATH)
    public ResponseEntity<List<StockSummaryResponse>> getTenantStockSummary(
            @PathVariable Long tenantId) {

        List<StockSummaryResponse> response = stockService.getTenantStockSummary(tenantId).stream()
                .map(s -> new StockSummaryResponse(
                        s.productId(),
                        s.sku(),
                        s.warehouseId(),
                        s.warehouseName(),
                        s.availableQuantity(),
                        s.reservedQuantity(),
                        s.lowStockThreshold()
                ))
                .toList();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update low-stock threshold", description = "Bir depo+ürün için düşük stok eşiğini günceller. Merchant uyarı/renk göstergesi bu eşiğe göre çalışır.")
    @ApiResponse(responseCode = "200", description = "Eşik güncellendi")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    @PatchMapping(ApiPaths.Stocks.TENANT_STOCKS_PATH + "/low-stock-threshold")
    public ResponseEntity<Void> updateLowStockThreshold(
            @PathVariable Long tenantId,
            @Valid @RequestBody UpdateThresholdRequest request) {

        stockService.updateLowStockThreshold(
                tenantId, request.warehouseId(), request.productId(), request.threshold());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get stock status", description = "Returns detailed stock status for a specific product in a specific warehouse.")
    @ApiResponse(responseCode = "200", description = "Stock detail")
    @ApiResponse(responseCode = "404", description = "No stock record found")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    @GetMapping(ApiPaths.Stocks.TENANT_STOCKS_PATH + "/product/{productId}/warehouse/{warehouseId}")
    public ResponseEntity<StockResponse> getStockStatus(
            @PathVariable Long tenantId,
            @PathVariable Long productId,
            @PathVariable Long warehouseId) {

        StockInfo stock = stockService.getStockInfo(tenantId, warehouseId, productId);

        StockResponse response = new StockResponse(
                stock.productId(),
                stock.sku(),
                stock.availableQuantity()
        );

        return ResponseEntity.ok(response);
    }
}