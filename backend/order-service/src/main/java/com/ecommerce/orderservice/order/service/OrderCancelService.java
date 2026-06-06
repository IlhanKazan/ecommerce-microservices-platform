package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.order.entity.Order;

import java.util.UUID;

public interface OrderCancelService {
    Order cancelOrder(Long orderId, UUID userId, String reason, String recipientEmail);
}
