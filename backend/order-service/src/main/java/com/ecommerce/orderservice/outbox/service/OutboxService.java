package com.ecommerce.orderservice.outbox.service;

import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;

import java.util.List;
import java.util.UUID;

public interface OutboxService {

    void publishOrderConfirmedEvent(Long orderId, UUID userId, Long tenantId,
            List<OrderItemSnapshotPayload> items, String recipientEmail);

    void publishOrderCancelledEvent(Long orderId, UUID userId, Long tenantId, String reason,
            List<OrderItemSnapshotPayload> items, String recipientEmail);

    void publishOrderShippedEvent(Long orderId, Long tenantId, String trackingNumber, String recipientEmail);

    void publishOrderRefundedEvent(Long orderId, UUID userId, Long tenantId, String reason, String recipientEmail);
}
