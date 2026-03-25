package com.ecommerce.stockservice.outbox.service;

public interface OutboxService {
    void publishStockStatusChangedEvent(String aggregateId, Long productId, boolean inStock, String status);
    void publishStockReservedEvent(String aggregateId, Long productId, int amount, String orderId);
}
