package com.ecommerce.contracts.event.order;

public record OrderShippedEventPayload(
        Long orderId,
        Long tenantId,
        String trackingNumber,
        String recipientEmail
) {}
