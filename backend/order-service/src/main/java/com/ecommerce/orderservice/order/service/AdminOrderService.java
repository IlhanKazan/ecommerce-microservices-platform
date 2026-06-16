package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.order.constant.OrderStatus;
import com.ecommerce.orderservice.order.controller.dto.response.AdminOrderDetailResponse;
import com.ecommerce.orderservice.order.controller.dto.response.AdminOrderStatsResponse;
import com.ecommerce.orderservice.order.controller.dto.response.AdminOrderSummaryResponse;
import com.ecommerce.orderservice.order.controller.dto.response.OrderDetailResponse;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderItem;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Platform admin sipariş yönetimi — listeleme, detay, istatistik.
 * Yetki controller'da (@PreAuthorize hasRole platform-admin).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Page<AdminOrderSummaryResponse> listOrders(OrderStatus status, Long tenantId, Pageable pageable) {
        return orderRepository.searchForAdmin(status, tenantId, pageable).map(this::toSummary);
    }

    public AdminOrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı: " + orderId, "ORDER_NOT_FOUND"));
        List<OrderDetailResponse.OrderItemDetailDto> items = orderItemRepository.findByOrderId(orderId).stream()
                .map(this::toItemDto).toList();
        return new AdminOrderDetailResponse(
                order.getId(), order.getTenantId(), order.getUserId(), order.getBuyerEmail(),
                order.getStatus().name(), order.getTotalAmount(), order.getCurrency(),
                order.getShippingAddressJson(), order.getCancellationReason(), order.getCreatedAt(), items);
    }

    public AdminOrderStatsResponse getStats() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (Object[] row : orderRepository.countByStatusGrouped()) {
            OrderStatus st = (OrderStatus) row[0];
            long count = (Long) row[1];
            byStatus.put(st.name(), count);
            total += count;
        }
        BigDecimal gmv = orderRepository.totalGmv();
        return new AdminOrderStatsResponse(total, gmv != null ? gmv : BigDecimal.ZERO, byStatus);
    }

    private AdminOrderSummaryResponse toSummary(Order o) {
        return new AdminOrderSummaryResponse(
                o.getId(), o.getTenantId(), o.getUserId(), o.getBuyerEmail(),
                o.getStatus().name(), o.getTotalAmount(), o.getCurrency(), o.getCreatedAt());
    }

    private OrderDetailResponse.OrderItemDetailDto toItemDto(OrderItem item) {
        return new OrderDetailResponse.OrderItemDetailDto(
                item.getProductId(), item.getSku(), item.getProductName(),
                item.getProductImageUrl(), item.getUnitPrice(), item.getQuantity());
    }
}
