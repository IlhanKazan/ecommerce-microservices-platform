package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.order.command.UpdateOrderStatusCommand;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.orderservice.order.service.OrderStatusService;
import com.ecommerce.orderservice.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusServiceImpl implements OrderStatusService {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public Order updateStatus(UpdateOrderStatusCommand command) {
        Order order = orderRepository.findByIdAndTenantId(command.orderId(), command.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı.", "ORDER_NOT_FOUND"));

        switch (command.newStatus()) {
            case "SHIPPED" -> {
                order.ship();
                outboxService.publishOrderShippedEvent(
                        order.getId(), order.getTenantId(), command.trackingNumber(), order.getBuyerEmail());
                log.info("Sipariş kargoya verildi. OrderID: {}", order.getId());
            }
            case "DELIVERED" -> {
                order.deliver();
                log.info("Sipariş teslim edildi. OrderID: {}", order.getId());
            }
            default -> throw new BusinessException(
                    "Geçersiz durum: " + command.newStatus() + ". Geçerli: SHIPPED, DELIVERED",
                    "INVALID_STATUS");
        }

        return orderRepository.save(order);
    }
}
