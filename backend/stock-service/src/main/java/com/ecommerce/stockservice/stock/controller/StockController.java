package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.stock.controller.dto.request.AddStockRequest;
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
                        s.reservedQuantity()
                ))
                .toList();

        return ResponseEntity.ok(response);
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