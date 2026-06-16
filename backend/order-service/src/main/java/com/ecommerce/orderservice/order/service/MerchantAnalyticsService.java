package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.order.controller.dto.response.MerchantAnalyticsResponse;
import com.ecommerce.orderservice.order.controller.dto.response.MerchantAnalyticsResponse.TopProduct;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Merchant satış analitiği — ürün-bazlı satış metrikleri + toplam ciro/sipariş/adet.
 * Yetki controller'da (@tenantSecurity.isMember).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MerchantAnalyticsService {

    private static final int TOP_PRODUCT_LIMIT = 10;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public MerchantAnalyticsResponse getTenantAnalytics(Long tenantId) {
        BigDecimal totalRevenue = orderRepository.tenantRevenue(tenantId);
        BigDecimal totalCommission = orderRepository.tenantCommission(tenantId);
        long totalOrders = orderRepository.tenantOrderCount(tenantId);
        long totalUnits = orderItemRepository.tenantTotalUnits(tenantId);

        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;
        if (totalCommission == null) totalCommission = BigDecimal.ZERO;
        BigDecimal totalNet = totalRevenue.subtract(totalCommission);

        List<TopProduct> topProducts = orderItemRepository
                .topProductsByTenant(tenantId, PageRequest.of(0, TOP_PRODUCT_LIMIT)).stream()
                .map(row -> new TopProduct(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO,
                        ((Number) row[4]).longValue()))
                .toList();

        return new MerchantAnalyticsResponse(
                totalRevenue, totalCommission, totalNet,
                totalOrders, totalUnits, topProducts);
    }
}
