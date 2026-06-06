package com.ecommerce.orderservice.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.orderservice.inbox.service.InboxService;
import com.ecommerce.orderservice.order.service.OrderCompensationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockEventConsumer {

    private final InboxService inboxService;
    private final OrderCompensationService compensationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EventConstants.AGGREGATE_STOCK, groupId = "order-service-group")
    public void consumeStockEvents(
            String messagePayload,
            @Header(value = "message_type", required = false) String eventType,
            ConsumerRecord<String, String> record) {

        String messageId = record.topic() + ":" + record.partition() + ":" + record.offset();
        log.info("STOCK event alındı — eventType: {}, messageId: {}", eventType, messageId);

        if (!EventConstants.EVENT_STOCK_COMMIT_FAILED.equals(eventType)) {
            log.debug("Order-service'i ilgilendirmeyen STOCK event: {}", eventType);
            return;
        }

        try {
            String json = objectMapper.readValue(messagePayload, String.class);

            if (inboxService.isMessageProcessed(messageId, eventType, json)) {
                return;
            }

            JsonNode node = objectMapper.readTree(json);
            Long orderId = node.get("orderId").asLong();
            String transactionId = node.has("transactionId") && !node.get("transactionId").isNull()
                    ? node.get("transactionId").asText() : null;
            String reason = node.has("reason") ? node.get("reason").asText("Bilinmeyen hata") : "Bilinmeyen hata";

            compensationService.handleStockCommitFailed(orderId, transactionId, reason);

        } catch (Exception e) {
            log.error("STOCK_COMMIT_FAILED_EVENT işlenirken hata — messageId: {}, hata: {}",
                    messageId, e.getMessage(), e);
        }
    }
}
