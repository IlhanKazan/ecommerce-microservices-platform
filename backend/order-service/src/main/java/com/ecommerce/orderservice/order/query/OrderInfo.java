package com.ecommerce.orderservice.order.query;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderInfo(
        Long id,
        UUID userId,
        Long tenantId,
        String status,
        BigDecimal totalAmount,
        String currency,
        String shippingAddressJson,
        String paymentTransactionId,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime deliveredAt,
        List<OrderItemInfo> items
) implements Serializable {}
