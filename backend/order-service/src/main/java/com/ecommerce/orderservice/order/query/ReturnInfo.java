package com.ecommerce.orderservice.order.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Merchant/admin iade listesi satırı (service çıktısı). */
public record ReturnInfo(
        Long returnId,
        Long orderId,
        Long tenantId,
        String reasonCode,
        String reason,
        String status,
        BigDecimal orderTotal,
        String buyerEmail,
        LocalDateTime createdAt
) {}
