package com.ecommerce.orderservice.outbox.service;

import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxService {

    void publishOrderConfirmedEvent(Long orderId, UUID userId, Long tenantId,
            List<OrderItemSnapshotPayload> items, String recipientEmail);

    void publishOrderCancelledEvent(Long orderId, UUID userId, Long tenantId, String reason,
            List<OrderItemSnapshotPayload> items, String recipientEmail);

    void publishOrderShippedEvent(Long orderId, Long tenantId, String trackingNumber, String recipientEmail);

    void publishOrderRefundedEvent(Long orderId, UUID userId, Long tenantId, String reason, String recipientEmail);

    void publishOrderDeliveredEvent(Long orderId, Long tenantId, String recipientEmail, LocalDateTime deliveredAt);

    void publishOrderReturnRequestedEvent(Long orderId, UUID userId, Long tenantId, String reason, String recipientEmail);

    void publishOrderReturnRejectedEvent(Long orderId, UUID userId, Long tenantId, String note, String recipientEmail);

    void publishOrderReturnedEvent(Long orderId, UUID userId, Long tenantId,
            List<OrderItemSnapshotPayload> items, BigDecimal refundAmount, String recipientEmail);
}
