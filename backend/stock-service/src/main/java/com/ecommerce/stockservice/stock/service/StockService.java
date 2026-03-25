package com.ecommerce.stockservice.stock.service;

import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.entity.StockInfo;

import java.util.UUID;

public interface StockService {
    void reserveStockForOrder(Long tenantId, Long warehouseId, Long productId, int amount, String orderId);
    void addManualStock(Long tenantId, Long warehouseId, Long productId, int amount, UUID userId);
    Stock getStock(Long tenantId, Long warehouseId, Long productId);
    StockInfo getStockInfo(Long tenantId, Long warehouseId, Long productId);
}
