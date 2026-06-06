package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderItem;
import com.ecommerce.orderservice.order.query.OrderInfo;
import com.ecommerce.orderservice.order.query.OrderItemInfo;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.orderservice.order.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderQueryServiceImpl implements OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderInfo> getMyOrders(UUID userId, int page, int size) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        return toPageResponse(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderInfo getMyOrderDetail(Long orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı.", "ORDER_NOT_FOUND"));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return toInfo(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderInfo> getTenantOrders(Long tenantId, int page, int size) {
        Page<Order> orders = orderRepository.findByTenantIdOrderByCreatedAtDesc(
                tenantId, PageRequest.of(page, size));
        return toPageResponse(orders);
    }

    private PageResponse<OrderInfo> toPageResponse(Page<Order> orders) {
        List<Long> ids = orders.getContent().stream().map(Order::getId).toList();
        Map<Long, List<OrderItem>> itemsMap = orderItemRepository.findByOrderIdIn(ids)
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<OrderInfo> infos = orders.getContent().stream()
                .map(o -> toInfo(o, itemsMap.getOrDefault(o.getId(), List.of())))
                .toList();

        return new PageResponse<>(
                infos,
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages(),
                orders.isLast()
        );
    }

    private OrderInfo toInfo(Order order, List<OrderItem> items) {
        List<OrderItemInfo> itemInfos = items.stream()
                .map(i -> new OrderItemInfo(
                        i.getId(), i.getProductId(), i.getSku(),
                        i.getProductName(), i.getProductImageUrl(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getTenantId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getShippingAddressJson(),
                order.getPaymentTransactionId(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                itemInfos
        );
    }
}
