package com.ecommerce.stockservice.stock.service;

import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.query.StockAvailabilityInfo;
import com.ecommerce.stockservice.stock.query.StockInfo;
import com.ecommerce.stockservice.stock.query.StockSummaryInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StockService {
    void reserveStockForOrder(Long tenantId, Long warehouseId, Long productId, int amount, String orderId);
    void addManualStock(Long tenantId, Long warehouseId, Long productId, int amount, UUID userId);
    void addManualStockBatch(Long tenantId, Long warehouseId, Map<Long, Integer> productAmounts, UUID userId);
    void removeManualStock(Long tenantId, Long warehouseId, Long productId, int amount, UUID userId);
    void updateLowStockThreshold(Long tenantId, Long warehouseId, Long productId, int threshold);
    Stock getStock(Long tenantId, Long warehouseId, Long productId);
    StockInfo getStockInfo(Long tenantId, Long warehouseId, Long productId);
    List<StockSummaryInfo> getTenantStockSummary(Long tenantId);

    // Public — ürün/varyant id listesi için stok durumu (detay sayfası "Tükendi" / "Son X adet")
    List<StockAvailabilityInfo> getAvailability(List<Long> productIds);

    int resyncStockStatus();

    int reconcileReservations();
}
