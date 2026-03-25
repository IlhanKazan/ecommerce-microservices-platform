package com.ecommerce.stockservice.stock.controller;

import com.ecommerce.common.annotation.CurrentUser;
import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.common.security.dto.AuthUser;
import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.stock.controller.dto.request.AddStockRequest;
import com.ecommerce.stockservice.stock.controller.dto.response.StockResponse;
import com.ecommerce.stockservice.stock.entity.StockInfo;
import com.ecommerce.stockservice.stock.service.StockService;
import com.ecommerce.stockservice.stock.entity.Stock;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.Stocks.STOCKS_PATH)
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

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