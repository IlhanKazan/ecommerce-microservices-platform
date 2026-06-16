package com.ecommerce.orderservice.order.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Merchant satış analitiği — toplam ciro/sipariş/adet + en çok satan ürünler.
 * Yalnız iptal/iade dışı (CONFIRMED/SHIPPED/DELIVERED) siparişler sayılır.
 */
public record MerchantAnalyticsResponse(
        BigDecimal totalRevenue,
        BigDecimal totalCommission,
        BigDecimal totalNet,
        long totalOrders,
        long totalUnits,
        List<TopProduct> topProducts
) {
    public record TopProduct(
            Long productId,
            String productName,
            long unitsSold,
            BigDecimal revenue,
            long orderCount
    ) {}
}
