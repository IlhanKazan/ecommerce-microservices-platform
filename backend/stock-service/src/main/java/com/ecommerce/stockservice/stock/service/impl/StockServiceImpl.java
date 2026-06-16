package com.ecommerce.stockservice.stock.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.stockservice.client.adapter.ProductClientAdapter;
import com.ecommerce.stockservice.client.dto.ProductResponse;
import com.ecommerce.stockservice.outbox.constant.TransactionType;
import com.ecommerce.stockservice.outbox.service.OutboxService;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.query.StockAvailabilityInfo;
import com.ecommerce.stockservice.stock.query.StockInfo;
import com.ecommerce.stockservice.stock.query.StockSummaryInfo;
import com.ecommerce.stockservice.stock.repository.ProductStockSumProjection;
import com.ecommerce.stockservice.stock.repository.StockRepository;
import com.ecommerce.stockservice.stock.service.SearchStockStatusPublisher;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockServiceImpl implements StockService {

    // Public availability'de tam adet yalnızca bu eşiğin altında gösterilir ("Son X adet").
    // Üstünde envanter gizli (inStock=true, qty=null) — rakip envanter sızıntısı önlenir.
    private static final int LOW_STOCK_DISPLAY_THRESHOLD = 10;

    private final StockRepository stockRepository;
    private final StockMovementService movementService;
    private final OutboxService outboxService;
    private final WarehouseService warehouseService;
    private final ProductClientAdapter productClientAdapter;
    private final SearchStockStatusPublisher searchStockStatusPublisher;

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
            // Varyantlı üründe parent inStock'u kalan varyantların stoğuna göre yeniden hesaplanır.
            searchStockStatusPublisher.publish(productId, stock.getId().toString());
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
        doAddStock(tenantId, warehouseId, productId, amount, userId);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stock", allEntries = true)
    public void addManualStockBatch(Long tenantId, Long warehouseId, Map<Long, Integer> productAmounts, UUID userId) {
        if (productAmounts == null || productAmounts.isEmpty()) {
            throw new BusinessException("Stok listesi boş olamaz.", "STOCK_BATCH_EMPTY");
        }
        log.info("Toplu manuel stok ekleme. Tenant: {}, Depo: {}, kalem sayısı: {}", tenantId, warehouseId, productAmounts.size());
        // Tek transaction — varyant tohumlaması atomik (biri patlarsa hepsi geri alınır)
        productAmounts.forEach((productId, amount) -> doAddStock(tenantId, warehouseId, productId, amount, userId));
    }

    // addManualStock ile batch'in ortak çekirdeği. @Transactional çağıran metoddan miras alınır
    // (private → self-invocation tuzağı yok; doAddStock kendi tx'ini açmaz).
    private void doAddStock(Long tenantId, Long warehouseId, Long productId, int amount, UUID userId) {

        log.info("Manuel stok ekleme işlemi. Tenant: {}, Product: {}, Amount: {}", tenantId, productId, amount);

        Warehouse warehouse = warehouseService.findByTenantIdAndId(tenantId, warehouseId)
                .orElseThrow(() -> new BusinessException("Geçersiz depo veya bu depoda yetkiniz yok!", "WAREHOUSE_NOT_FOUND"));

        if (Boolean.FALSE.equals(warehouse.getIsActive())) {
            throw new BusinessException("Pasif depoya stok eklenemez. Önce depoyu aktifleştirin.", "WAREHOUSE_INACTIVE");
        }

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

        // 0'dan pozitife geçtiyse aggregate inStock değişebilir → parent dokümanını senkronla.
        // (Zaten pozitifti ise aggregate halihazırda true; tekrar yayınlamaya gerek yok.)
        if (oldQty == 0 && stock.getAvailableQuantity() > 0) {
            searchStockStatusPublisher.publish(productId, stock.getId().toString());
        }
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stock", key = "#tenantId + ':' + #warehouseId + ':' + #productId")
    public void removeManualStock(Long tenantId, Long warehouseId, Long productId, int amount, UUID userId) {

        log.info("Manuel stok düşürme işlemi. Tenant: {}, Product: {}, Amount: {}", tenantId, productId, amount);

        warehouseService.findByTenantIdAndId(tenantId, warehouseId)
                .orElseThrow(() -> new BusinessException("Geçersiz depo veya bu depoda yetkiniz yok!", "WAREHOUSE_NOT_FOUND"));

        Stock stock = stockRepository.findByTenantIdAndWarehouseIdAndProductId(tenantId, warehouseId, productId)
                .orElseThrow(() -> new BusinessException("Stok kaydı bulunamadı!", "STOCK_NOT_FOUND"));

        int oldQty = stock.getAvailableQuantity();
        stock.removeStock(amount);

        stock = stockRepository.save(stock);
        movementService.recordMovement(stock, TransactionType.MANUAL_ADJUSTMENT, String.valueOf(userId), -amount);

        if (oldQty > 0 && stock.getAvailableQuantity() == 0) {
            searchStockStatusPublisher.publish(productId, stock.getId().toString());
        }

        log.info("Stok başarıyla düşürüldü. Tenant: {}, Product: {}, YeniMiktar: {}", tenantId, productId, stock.getAvailableQuantity());
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

    @Override
    @Transactional
    public int resyncStockStatus() {
        List<Stock> positiveStocks = stockRepository.findAllWithPositiveQuantity();
        for (Stock stock : positiveStocks) {
            searchStockStatusPublisher.publish(stock.getProductId(), stock.getId().toString());
        }
        log.info("Stok resync tamamlandı. {} ürün için STOCK_STATUS_CHANGED event yazıldı.", positiveStocks.size());
        return positiveStocks.size();
    }

    // Asılı kalmış (sızmış) rezervasyonları serbest bırakan admin aracı.
    // SAGA reserve→commit/rollback arasında patlayan checkout'lardan kalan reservedQuantity'leri
    // availableQuantity'ye geri verir. DİKKAT: in-flight (devam eden) checkout yokken çalıştırılmalı,
    // aksi halde işlenmekte olan bir siparişin rezervesi de serbest bırakılabilir.
    @Override
    @Transactional
    public int reconcileReservations() {
        List<Stock> stuck = stockRepository.findAllWithReservedQuantity();
        log.warn("[RECONCILE] {} stok kaydında asılı rezervasyon bulundu, serbest bırakılıyor...", stuck.size());
        for (Stock stock : stuck) {
            int wasAvailable = stock.getAvailableQuantity();
            int released = stock.releaseAllReserved();
            if (released <= 0) {
                continue;
            }
            movementService.recordMovement(stock, TransactionType.RESERVATION_RECONCILED, "RECONCILE", released);
            stockRepository.save(stock);

            // 0'dan pozitife geçtiyse ürün tekrar stokta → ES senkronu (parent aggregate)
            if (wasAvailable == 0 && stock.getAvailableQuantity() > 0) {
                searchStockStatusPublisher.publish(stock.getProductId(), stock.getId().toString());
            }
            log.info("[RECONCILE] StockID: {}, ProductID: {}, serbest bırakılan: {}",
                    stock.getId(), stock.getProductId(), released);
        }
        log.warn("[RECONCILE] Tamamlandı. {} kayıt işlendi.", stuck.size());
        return stuck.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockSummaryInfo> getTenantStockSummary(Long tenantId) {
        return stockRepository.findAllByTenantIdWithWarehouse(tenantId).stream()
                .map(s -> new StockSummaryInfo(
                        s.getProductId(),
                        s.getSku(),
                        s.getWarehouse().getId(),
                        s.getWarehouse().getName(),
                        s.getAvailableQuantity(),
                        s.getReservedQuantity(),
                        s.getLowStockThreshold()
                ))
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "stock", key = "#tenantId + ':' + #warehouseId + ':' + #productId")
    public void updateLowStockThreshold(Long tenantId, Long warehouseId, Long productId, int threshold) {
        Stock stock = stockRepository.findByTenantIdAndWarehouseIdAndProductId(tenantId, warehouseId, productId)
                .orElseThrow(() -> new BusinessException("Stok kaydı bulunamadı!", "STOCK_NOT_FOUND"));
        stock.updateLowStockThreshold(threshold); // domain guard: negatif eşik reddedilir
        stockRepository.save(stock);
        log.info("Düşük stok eşiği güncellendi. Tenant: {}, Product: {}, Eşik: {}", tenantId, productId, threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAvailabilityInfo> getAvailability(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = productIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> sums = stockRepository.sumAvailableByProductIds(ids).stream()
                .collect(Collectors.toMap(
                        ProductStockSumProjection::getProductId,
                        p -> p.getQty() == null ? 0L : p.getQty()));

        return ids.stream().map(id -> {
            long qty = sums.getOrDefault(id, 0L);
            boolean inStock = qty > 0;
            // Tam adet sadece düşük stokta açığa çıkar; üstünde gizli (null)
            Integer displayQty = (inStock && qty <= LOW_STOCK_DISPLAY_THRESHOLD) ? (int) qty : null;
            return new StockAvailabilityInfo(id, inStock, displayQty);
        }).toList();
    }
}
