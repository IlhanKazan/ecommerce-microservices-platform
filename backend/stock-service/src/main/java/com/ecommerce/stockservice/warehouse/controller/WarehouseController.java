package com.ecommerce.stockservice.warehouse.controller;

import com.ecommerce.stockservice.common.constants.ApiPaths;
import com.ecommerce.stockservice.warehouse.controller.dto.request.WarehouseCreateRequest;
import com.ecommerce.stockservice.warehouse.controller.dto.response.WarehouseResponse;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.mapper.WarehouseMapper;
import com.ecommerce.stockservice.warehouse.service.WarehouseService;
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
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WarehouseMapper warehouseMapper;

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

    @GetMapping
    @PreAuthorize("@tenantSecurity.isMember(#tenantId)")
    public ResponseEntity<List<WarehouseResponse>> getWarehouses(@PathVariable Long tenantId) {

        List<Warehouse> warehouses = warehouseService.getWarehousesByTenant(tenantId);

        return ResponseEntity.ok(warehouseMapper.toResponseList(warehouses));
    }
}