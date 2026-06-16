package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.order.controller.dto.response.ProductSalesMetricsResponse;
import com.ecommerce.orderservice.order.controller.dto.response.ProductSalesMetricsResponse.ProductMetricRow;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ürün-bazlı satış metriği. Varyant ilişkisini product-service'ten (Feign) çözüp
 * parent + tüm varyant id'leri üzerinden agregasyon yapar.
 * Yetki controller'da: merchant @tenantSecurity.isMember (tenantId dolu) / admin platform-admin (tenantId null).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductMetricsService {

    private final ProductServiceClient productServiceClient;
    private final OrderItemRepository orderItemRepository;

    public ProductSalesMetricsResponse getMetrics(Long productId, Long tenantId) {
        // Ürünün kendisi + tüm varyant id'leri (product-service çözer)
        List<Long> ids = productServiceClient.getSalesIds(productId);

        List<ProductMetricRow> breakdown = orderItemRepository.metricsByProductIds(ids, tenantId).stream()
                .map(row -> new ProductMetricRow(
                        (Long) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO,
                        ((Number) row[4]).longValue()))
                .toList();

        long totalUnits = breakdown.stream().mapToLong(ProductMetricRow::unitsSold).sum();
        BigDecimal totalRevenue = breakdown.stream()
                .map(ProductMetricRow::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalOrders = orderItemRepository.distinctOrderCountByProductIds(ids, tenantId);

        return new ProductSalesMetricsResponse(productId, totalUnits, totalRevenue, totalOrders, breakdown);
    }
}
