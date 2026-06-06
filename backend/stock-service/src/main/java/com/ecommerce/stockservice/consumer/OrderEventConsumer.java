package com.ecommerce.stockservice.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.contracts.event.order.OrderCancelledEventPayload;
import com.ecommerce.contracts.event.order.OrderConfirmedEventPayload;
import com.ecommerce.stockservice.inbox.service.InboxService;
import com.ecommerce.stockservice.stock.service.InternalStockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InboxService inboxService;
    private final InternalStockService internalStockService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EventConstants.AGGREGATE_ORDER, groupId = "stock-service-group")
    public void consumeOrderEvents(ConsumerRecord<String, String> record) {
        String eventType = extractHeader(record, "message_type");
        String messageId = record.topic() + ":" + record.partition() + ":" + record.offset();

        log.info("ORDER event alındı — eventType: {}, messageId: {}", eventType, messageId);

        try {
            String json = objectMapper.readValue(record.value(), String.class);

            if (inboxService.isMessageProcessed(messageId, eventType, json)) {
                return;
            }

            switch (eventType) {
                case EventConstants.EVENT_ORDER_CONFIRMED -> {
                    OrderConfirmedEventPayload payload =
                            objectMapper.readValue(json, OrderConfirmedEventPayload.class);
                    if (payload.items() != null && !payload.items().isEmpty()) {
                        internalStockService.commitAllForOrder(
                                payload.orderId().toString(), payload.tenantId(), payload.items());
                        log.info("Stok commit tamamlandı. OrderID: {}", payload.orderId());
                    } else {
                        log.warn("ORDER_CONFIRMED_EVENT items boş geldi. OrderID: {}", payload.orderId());
                    }
                }
                case EventConstants.EVENT_ORDER_CANCELLED -> {
                    OrderCancelledEventPayload payload =
                            objectMapper.readValue(json, OrderCancelledEventPayload.class);
                    if (payload.items() != null && !payload.items().isEmpty()) {
                        internalStockService.cancelAllForOrder(
                                payload.orderId().toString(), payload.tenantId(), payload.items());
                        log.info("İptal stok rollback tamamlandı. OrderID: {}", payload.orderId());
                    } else {
                        log.warn("ORDER_CANCELLED_EVENT items boş geldi. OrderID: {}", payload.orderId());
                    }
                }
                case null, default ->
                        log.debug("Stock-service'i ilgilendirmeyen ORDER event: {}", eventType);
            }

        } catch (Exception e) {
            log.error("ORDER event işlenirken hata — eventType: {}, messageId: {}, hata: {}",
                    eventType, messageId, e.getMessage(), e);
            // Exception fırlatılmıyor — sonsuz retry'a düşmesin
        }
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
