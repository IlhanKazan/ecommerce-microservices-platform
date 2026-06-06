package com.ecommerce.orderservice.order.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.orderservice.order.query.OrderInfo;

import java.util.UUID;

public interface OrderQueryService {
    PageResponse<OrderInfo> getMyOrders(UUID userId, int page, int size);
    OrderInfo getMyOrderDetail(Long orderId, UUID userId);
    PageResponse<OrderInfo> getTenantOrders(Long tenantId, int page, int size);
}
