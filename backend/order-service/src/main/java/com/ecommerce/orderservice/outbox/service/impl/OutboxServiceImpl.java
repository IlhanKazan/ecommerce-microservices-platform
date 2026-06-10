package com.ecommerce.orderservice.outbox.service.impl;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.common.exception.SystemException;
import com.ecommerce.contracts.event.order.*;
import com.ecommerce.orderservice.outbox.entity.Outbox;
import com.ecommerce.orderservice.outbox.repository.OutboxRepository;
import com.ecommerce.orderservice.outbox.service.OutboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderConfirmedEvent(Long orderId, UUID userId, Long tenantId,
            List<OrderItemSnapshotPayload> items, String recipientEmail) {
        publish(
                orderId.toString(),
                EventConstants.EVENT_ORDER_CONFIRMED,
                new OrderConfirmedEventPayload(orderId, userId, tenantId, items, recipientEmail)
        );
        log.info("ORDER_CONFIRMED_EVENT outbox'a yazıldı. OrderID: {}, ItemCount: {}", orderId, items != null ? items.size() : 0);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderCancelledEvent(Long orderId, UUID userId, Long tenantId, String reason,
            List<OrderItemSnapshotPayload> items, String recipientEmail) {
        publish(
                orderId.toString(),
                EventConstants.EVENT_ORDER_CANCELLED,
                new OrderCancelledEventPayload(orderId, userId, tenantId, reason, items, recipientEmail)
        );
        log.info("ORDER_CANCELLED_EVENT outbox'a yazıldı. OrderID: {}, Sebep: {}", orderId, reason);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderShippedEvent(Long orderId, Long tenantId, String trackingNumber, String recipientEmail) {
        publish(
                orderId.toString(),
                EventConstants.EVENT_ORDER_SHIPPED,
                new OrderShippedEventPayload(orderId, tenantId, trackingNumber, recipientEmail)
        );
        log.info("ORDER_SHIPPED_EVENT outbox'a yazıldı. OrderID: {}, Kargo No: {}", orderId, trackingNumber);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderRefundedEvent(Long orderId, UUID userId, Long tenantId, String reason, String recipientEmail) {
        publish(
                orderId.toString(),
                EventConstants.EVENT_ORDER_REFUNDED,
                new OrderRefundedEventPayload(orderId, userId, tenantId, reason, recipientEmail)
        );
        log.info("ORDER_REFUNDED_EVENT outbox'a yazıldı. OrderID: {}", orderId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderDeliveredEvent(Long orderId, Long tenantId, String recipientEmail, LocalDateTime deliveredAt) {
        publish(
                orderId.toString(),
                EventConstants.EVENT_ORDER_DELIVERED,
                new OrderDeliveredEventPayload(orderId, tenantId, recipientEmail, deliveredAt)
        );
        log.info("ORDER_DELIVERED_EVENT outbox'a yazıldı. OrderID: {}", orderId);
    }

    private void publish(String aggregateId, String messageType, Object payload) {
        try {
            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_ORDER)
                    .aggregateId(aggregateId)
                    .messageType(messageType)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası. MessageType: {}, Hata: {}", messageType, e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }
}
