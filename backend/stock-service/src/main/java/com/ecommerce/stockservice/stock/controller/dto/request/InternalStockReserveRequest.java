package com.ecommerce.stockservice.stock.controller.dto.request;

import java.util.List;

public record InternalStockReserveRequest(
        String orderId,
        Long tenantId,
        List<StockItemRequest> items
) {
    public record StockItemRequest(Long productId, int quantity) {}
}
