package com.ecommerce.stockservice.stock.service;

import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;
import com.ecommerce.stockservice.stock.controller.dto.request.InternalStockReserveRequest.StockItemRequest;

import java.util.List;

public interface InternalStockService {
    void reserveAllForOrder(String orderId, Long tenantId, List<StockItemRequest> items);
    void rollbackAllForOrder(String orderId, Long tenantId, List<StockItemRequest> items);
    void commitAllForOrder(String orderId, Long tenantId, List<OrderItemSnapshotPayload> items);
    void cancelAllForOrder(String orderId, Long tenantId, List<OrderItemSnapshotPayload> items);
}
