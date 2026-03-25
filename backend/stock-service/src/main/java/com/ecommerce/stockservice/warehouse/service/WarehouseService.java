package com.ecommerce.stockservice.warehouse.service;

import com.ecommerce.stockservice.warehouse.entity.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseService {
    Warehouse saveAndFlush(Warehouse warehouse);
    Optional<Warehouse> findByTenantIdAndId(Long tenantId, Long warehouseId);
    Warehouse createWarehouse(Long tenantId, String code, String name, String locationDetails);
    List<Warehouse> getWarehousesByTenant(Long tenantId);
}
