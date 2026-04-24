package com.ecommerce.stockservice.stock.query;

import java.io.Serializable;

public record StockInfo(
        Long productId,
        String sku,
        Integer availableQuantity,
        Integer reservedQuantity
) implements Serializable {
}