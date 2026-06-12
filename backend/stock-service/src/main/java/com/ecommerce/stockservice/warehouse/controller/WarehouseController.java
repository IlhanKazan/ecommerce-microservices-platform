package com.ecommerce.stockservice.warehouse.controller;

import com.ecommerce.common.annotation.Idempotent;
import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.warehouse.controller.dto.request.WarehouseCreateRequest;
import com.ecommerce.stockservice.warehouse.controller.dto.request.WarehouseStatusUpdateRequest;
import com.ecommerce.stockservice.warehouse.controller.dto.request.WarehouseUpdateRequest;
import com.ecommerce.stockservice.warehouse.controller.dto.response.WarehouseResponse;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.mapper.WarehouseMapper;
import com.ecommerce.stockservice.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Warehouses.WAREHOUSES_PATH)
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Warehouse management per tenant — create, list, detail, update, activate/deactivate and delete warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WarehouseMapper warehouseMapper;

    @Operation(summary = "Create warehouse", description = "Creates a new warehouse location for the tenant. Warehouses are used to track inventory by physical location.")
    @ApiResponse(responseCode = "201", description = "Warehouse created")
    @Idempotent(cachePrefix = "idempotency:warehouse-create:", ttlSeconds = 300)
    @PostMapping
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<WarehouseResponse> createWarehouse(
            @PathVariable Long tenantId,
            @Valid @RequestBody WarehouseCreateRequest request) {

        Warehouse warehouse = warehouseService.createWarehouse(
                tenantId, request.code(), request.name(), request.locationDetails()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(warehouseMapper.toResponse(warehouse));
    }

    @Operation(summary = "List warehouses", description = "Returns all warehouses for this tenant.")
    @ApiResponse(responseCode = "200", description = "Warehouse list")
    @GetMapping
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<List<WarehouseResponse>> getWarehouses(@PathVariable Long tenantId) {

        List<Warehouse> warehouses = warehouseService.getWarehousesByTenant(tenantId);

        return ResponseEntity.ok(warehouseMapper.toResponseList(warehouses));
    }

    @Operation(summary = "Get warehouse", description = "Returns a single warehouse by id for this tenant.")
    @ApiResponse(responseCode = "200", description = "Warehouse detail")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @GetMapping("/{warehouseId}")
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<WarehouseResponse> getWarehouse(
            @PathVariable Long tenantId,
            @PathVariable Long warehouseId) {

        Warehouse warehouse = warehouseService.getWarehouse(tenantId, warehouseId);

        return ResponseEntity.ok(warehouseMapper.toResponse(warehouse));
    }

    @Operation(summary = "Update warehouse", description = "Updates the name and location of a warehouse. The warehouse code is immutable.")
    @ApiResponse(responseCode = "200", description = "Warehouse updated")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @Idempotent(cachePrefix = "idempotency:warehouse-update:", ttlSeconds = 300)
    @PutMapping("/{warehouseId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<WarehouseResponse> updateWarehouse(
            @PathVariable Long tenantId,
            @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseUpdateRequest request) {

        Warehouse warehouse = warehouseService.updateWarehouse(
                tenantId, warehouseId, request.name(), request.locationDetails()
        );

        return ResponseEntity.ok(warehouseMapper.toResponse(warehouse));
    }

    @Operation(summary = "Activate/deactivate warehouse", description = "Toggles the active state of a warehouse. A deactivated warehouse keeps its existing stock but rejects new stock additions.")
    @ApiResponse(responseCode = "200", description = "Warehouse status updated")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @Idempotent(cachePrefix = "idempotency:warehouse-status:", ttlSeconds = 300)
    @PatchMapping("/{warehouseId}/status")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<WarehouseResponse> updateWarehouseStatus(
            @PathVariable Long tenantId,
            @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseStatusUpdateRequest request) {

        Warehouse warehouse = warehouseService.setActive(tenantId, warehouseId, request.active());

        return ResponseEntity.ok(warehouseMapper.toResponse(warehouse));
    }

    @Operation(summary = "Delete warehouse", description = "Permanently deletes a warehouse. Only allowed when the warehouse holds no stock records; otherwise returns 409.")
    @ApiResponse(responseCode = "204", description = "Warehouse deleted")
    @ApiResponse(responseCode = "404", description = "Warehouse not found")
    @ApiResponse(responseCode = "409", description = "Warehouse still holds stock")
    @DeleteMapping("/{warehouseId}")
    @PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
    public ResponseEntity<Void> deleteWarehouse(
            @PathVariable Long tenantId,
            @PathVariable Long warehouseId) {

        warehouseService.deleteWarehouse(tenantId, warehouseId);

        return ResponseEntity.noContent().build();
    }
}