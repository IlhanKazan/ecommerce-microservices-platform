package com.ecommerce.stockservice.stock.query;

import java.io.Serializable;

public record StockSummaryInfo(
        Long productId,
        String sku,
        Long warehouseId,
        String warehouseName,
        Integer availableQuantity,
        Integer reservedQuantity,
        Integer lowStockThreshold
) implements Serializable {
}
