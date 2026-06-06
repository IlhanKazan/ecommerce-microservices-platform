package com.ecommerce.orderservice.client.dto;

import java.util.List;

public record StockReserveRequest(
        String orderId,
        Long tenantId,
        List<StockItemDto> items
) {
    public record StockItemDto(Long productId, int quantity) {}
}
