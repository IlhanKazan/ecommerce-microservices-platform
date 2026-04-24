package com.ecommerce.stockservice.stock.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.stockservice.client.adapter.ProductClientAdapter;
import com.ecommerce.stockservice.client.dto.ProductResponse;
import com.ecommerce.stockservice.outbox.constant.TransactionType;
import com.ecommerce.stockservice.outbox.service.OutboxService;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.query.StockInfo;
import com.ecommerce.stockservice.stock.repository.StockRepository;
import com.ecommerce.stockservice.stock.service.StockService;
import com.ecommerce.stockservice.stockmovement.service.StockMovementService;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementService movementService;
    private final OutboxService outboxService;
    private final WarehouseService warehouseService;
    private final ProductClientAdapter productClientAdapter;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stock", key = "#tenantId + ':' + #warehouseId + ':' + #productId")
    public void reserveStockForOrder(Long tenantId, Long warehouseId, Long productId, int amount, String orderId) {

        log.info("Sipariş için stok rezervasyonu başlıyor. OrderID: {}, ProductID: {}", orderId, productId);

        Stock stock = stockRepository.findWithLockingByTenantIdAndWarehouseIdAndProductId(tenantId, warehouseId, productId)
                .orElseThrow(() -> new BusinessException("İlgili depoda ürün bulunamadı!", "STOCK_NOT_FOUND"));

        int oldQty = stock.getAvailableQuantity();
        stock.reserve(amount);

        if (oldQty > 0 && stock.getAvailableQuantity() == 0) {
            outboxService.publishStockStatusChangedEvent(
                    stock.getId().toString(),
                    productId,
                    false,
                    "OUT_OF_STOCK"
            );
        }

        movementService.recordMovement(stock, TransactionType.RESERVED_FOR_ORDER, orderId, -amount);

        outboxService.publishStockReservedEvent(
                stock.getId().toString(),
                productId,
                amount,
                orderId
        );

        stockRepository.save(stock);
        log.info("Stok başarıyla rezerve edildi. OrderID: {}", orderId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stock", key = "#tenantId + ':' + #warehouseId + ':' + #productId")
    public void addManualStock(Long tenantId, Long warehouseId, Long productId, int amount, UUID userId) {

        log.info("Manuel stok ekleme işlemi. Tenant: {}, Product: {}, Amount: {}", tenantId, productId, amount);

        Warehouse warehouse = warehouseService.findByTenantIdAndId(tenantId, warehouseId)
                .orElseThrow(() -> new BusinessException("Geçersiz depo veya bu depoda yetkiniz yok!", "WAREHOUSE_NOT_FOUND"));

        ProductResponse product = productClientAdapter.validateAndGetProduct(productId, tenantId);

        Stock stock = stockRepository.findByTenantIdAndWarehouseIdAndProductId(tenantId, warehouseId, productId)
                .orElseGet(() -> Stock.builder()
                        .tenantId(tenantId)
                        .warehouse(warehouse)
                        .productId(productId)
                        .sku(product.sku())
                        .availableQuantity(0)
                        .lowStockThreshold(5)
                        .build());

        int oldQty = stock.getAvailableQuantity();

        stock.addStock(amount);

        stock = stockRepository.save(stock);
        movementService.recordMovement(stock, TransactionType.MANUAL_ADJUSTMENT, String.valueOf(userId), amount);

        if (oldQty == 0 && stock.getAvailableQuantity() > 0) {
            outboxService.publishStockStatusChangedEvent(
                    stock.getId().toString(),
                    productId,
                    true,
                    "RESTOCKED"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Stock getStock(Long tenantId, Long warehouseId, Long productId) {
        return stockRepository.findByTenantIdAndWarehouseIdAndProductId(tenantId, warehouseId, productId)
                .orElseThrow(() -> new BusinessException("Stok kaydı bulunamadı!", "STOCK_NOT_FOUND"));
    }

    @Override
    @Cacheable(cacheNames = "stock", key = "#tenantId + ':' + #warehouseId + ':' + #productId")
    @Transactional(readOnly = true)
    public StockInfo getStockInfo(Long tenantId, Long warehouseId, Long productId) {

        Stock stock = this.getStock(tenantId, warehouseId, productId);

        // Entityi saf domain modeline çevirip dönüyoruz. Redis bunu saklayacak.
        return new StockInfo(
                stock.getProductId(),
                stock.getSku(),
                stock.getAvailableQuantity(),
                stock.getReservedQuantity()
        );
    }
}
