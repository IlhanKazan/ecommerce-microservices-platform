package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Tek ürünün satış metriği. Varyantlı üründe toplamlar tüm varyantları kapsar;
 * {@code breakdown} her satılan varyant/ürün için ayrı satır.
 */
public record ProductSalesMetricsResponse(
        Long productId,
        long totalUnits,
        BigDecimal totalRevenue,
        long totalOrders,
        List<ProductMetricRow> breakdown
) {
    public record ProductMetricRow(
            Long productId,
            String productName,
            long unitsSold,
            BigDecimal revenue,
            long orderCount
    ) {}
}
