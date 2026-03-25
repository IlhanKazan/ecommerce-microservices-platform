package com.ecommerce.stockservice.warehouse.service.impl;

import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.repository.WarehouseRepository;
import com.ecommerce.stockservice.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    public Warehouse saveAndFlush(Warehouse warehouse) {
        return warehouseRepository.saveAndFlush(warehouse);
    }

    @Override
    public Optional<Warehouse> findByTenantIdAndId(Long tenantId, Long warehouseId) {
        return warehouseRepository.findByTenantIdAndId(tenantId, warehouseId);
    }

    @Override
    public Warehouse createWarehouse(Long tenantId, String code, String name, String locationDetails) {
        Warehouse warehouse = Warehouse.builder()
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .locationDetails(locationDetails)
                .isActive(true)
                .build();
        return warehouseRepository.save(warehouse);
    }

    @Override
    public List<Warehouse> getWarehousesByTenant(Long tenantId) {
        return warehouseRepository.findAllByTenantId(tenantId);
    }

}
