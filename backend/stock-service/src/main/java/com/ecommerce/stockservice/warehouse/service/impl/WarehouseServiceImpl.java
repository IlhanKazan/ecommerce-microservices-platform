package com.ecommerce.stockservice.warehouse.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.stockservice.common.exception.WarehouseNotEmptyException;
import com.ecommerce.stockservice.outbox.service.OutboxService;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.repository.StockRepository;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.repository.WarehouseRepository;
import com.ecommerce.stockservice.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockRepository stockRepository;
    private final OutboxService outboxService;

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

    @Override
    public Warehouse getWarehouse(Long tenantId, Long warehouseId) {
        return findOwnedOrThrow(tenantId, warehouseId);
    }

    @Override
    @Transactional
    public Warehouse updateWarehouse(Long tenantId, Long warehouseId, String name, String locationDetails) {
        Warehouse warehouse = findOwnedOrThrow(tenantId, warehouseId);
        warehouse.setName(name);
        warehouse.setLocationDetails(locationDetails);
        log.info("Depo güncellendi. Tenant: {}, Warehouse: {}", tenantId, warehouseId);
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional
    public Warehouse setActive(Long tenantId, Long warehouseId, boolean active) {
        Warehouse warehouse = findOwnedOrThrow(tenantId, warehouseId);

        if (Boolean.valueOf(active).equals(warehouse.getIsActive())) {
            return warehouse; // durum zaten aynı — no-op, gereksiz event yayma
        }

        warehouse.setIsActive(active);
        warehouseRepository.saveAndFlush(warehouse); // aggregate sorgusu yeni isActive durumunu görsün diye flush

        propagateAvailability(tenantId, warehouseId, active);

        log.info("Depo durumu değişti. Tenant: {}, Warehouse: {}, active: {}", tenantId, warehouseId, active);
        return warehouse;
    }

    /**
     * Depo aktif/pasif olunca, içindeki stoğu olan ürünlerin satılabilirliğini ES'e yansıtır.
     * inStock = ürünün HERHANGİ bir AKTİF depoda availableQuantity>0 stoğu olması.
     */
    private void propagateAvailability(Long tenantId, Long warehouseId, boolean active) {
        List<Stock> affected = stockRepository.findPositiveStocksByWarehouse(tenantId, warehouseId);
        int published = 0;
        for (Stock stock : affected) {
            Long productId = stock.getProductId();
            if (active) {
                // Depo geri açıldı; bu depodaki pozitif stok ürünü yeniden satılabilir yapar.
                outboxService.publishStockStatusChangedEvent(
                        stock.getId().toString(), productId, true, "WAREHOUSE_REACTIVATED");
                published++;
            } else if (!stockRepository.existsAvailableInActiveWarehouse(tenantId, productId)) {
                // Depo kapandı ve başka aktif depoda stoğu yok; ürün satıştan kalkar.
                outboxService.publishStockStatusChangedEvent(
                        stock.getId().toString(), productId, false, "WAREHOUSE_DEACTIVATED");
                published++;
            }
        }
        log.info("Depo durum değişikliği {} stok kaydından {} ürünün satılabilirliğini değiştirdi. Tenant: {}, Warehouse: {}, active: {}",
                affected.size(), published, tenantId, warehouseId, active);
    }

    @Override
    @Transactional
    public void deleteWarehouse(Long tenantId, Long warehouseId) {
        Warehouse warehouse = findOwnedOrThrow(tenantId, warehouseId);
        if (stockRepository.existsByTenantIdAndWarehouseId(tenantId, warehouseId)) {
            throw new WarehouseNotEmptyException(
                    "Bu depoda stok kayıtları var, önce stokları boşaltın.", "WAREHOUSE_HAS_STOCK");
        }
        warehouseRepository.delete(warehouse);
        log.info("Depo silindi. Tenant: {}, Warehouse: {}", tenantId, warehouseId);
    }

    private Warehouse findOwnedOrThrow(Long tenantId, Long warehouseId) {
        return warehouseRepository.findByTenantIdAndId(tenantId, warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Depo bulunamadı veya bu depoda yetkiniz yok!", "WAREHOUSE_NOT_FOUND"));
    }

}
