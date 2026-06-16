package com.ecommerce.orderservice.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.orderservice.order.query.OrderInfo;

import java.util.List;
import java.util.UUID;

public interface OrderQueryService {
    PageResponse<OrderInfo> getMyOrders(UUID userId, int page, int size);
    OrderInfo getMyOrderDetail(Long orderId, UUID userId);
    PageResponse<OrderInfo> getTenantOrders(Long tenantId, String status, String q, int page, int size);
    // "Birlikte sıkça alınanlar" — co-purchase: ürünle aynı siparişte geçen diğer ürün id'leri (sıklığa göre).
    List<Long> getFrequentlyBoughtWith(Long productId, int limit);
}
