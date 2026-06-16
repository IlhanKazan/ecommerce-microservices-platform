package com.ecommerce.contracts.event.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * İade onaylandı, para iadesi yapıldı → stock-service stoğu geri ekler, mail-service "iadeniz tamamlandı" yollar.
 * {@code items} restock için (productId+quantity); {@code refundAmount} iade edilen tutar.
 */
public record OrderReturnedEventPayload(
        Long orderId,
        UUID userId,
        Long tenantId,
        List<OrderItemSnapshotPayload> items,
        BigDecimal refundAmount,
        String recipientEmail
) {}
