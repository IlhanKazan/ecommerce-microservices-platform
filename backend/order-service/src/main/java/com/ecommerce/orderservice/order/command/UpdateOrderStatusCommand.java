package com.ecommerce.orderservice.order.command;

public record UpdateOrderStatusCommand(
        Long orderId,
        Long tenantId,
        String newStatus,
        String trackingNumber
) {}
