package com.ecommerce.productservice.order.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.contracts.event.order.OrderConfirmedEventPayload;
import com.ecommerce.productservice.inbox.service.InboxService;
import com.ecommerce.productservice.product.service.InternalProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * ORDER event'lerini dinler; yalnızca ORDER_CONFIRMED'da satılan kalemlerin sale_count'unu artırır
 * (popülerlik / "çok satanlar"). Inbox ile idempotent — aynı sipariş yeniden işlenirse çift sayılmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final InboxService inboxService;
    private final InternalProductService internalProductService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EventConstants.AGGREGATE_ORDER, groupId = "product-service-group")
    public void consumeOrderEvents(ConsumerRecord<String, String> record) {
        String eventType = extractHeader(record, "message_type");

        // Sadece onaylanan sipariş sale_count'u besler; diğer ORDER event'leri ilgisiz.
        if (!EventConstants.EVENT_ORDER_CONFIRMED.equals(eventType)) {
            return;
        }

        // Benzersiz messageId: topic + partition + offset (Debezium header'da ID taşımaz)
        String messageId = record.topic() + ":" + record.partition() + ":" + record.offset();
        log.info("ORDER_CONFIRMED alındı (sale_count) — messageId: {}", messageId);

        try {
            String json = objectMapper.readValue(record.value(), String.class);

            if (inboxService.isAlreadyProcessed(messageId, eventType, json)) {
                return;
            }

            OrderConfirmedEventPayload payload =
                    objectMapper.readValue(json, OrderConfirmedEventPayload.class);
            internalProductService.recordSales(payload.items());

        } catch (Exception e) {
            // Exception fırlatılmıyor — Kafka'da sonsuz retry'a düşmesin (sale_count kritik değil).
            log.error("ORDER_CONFIRMED işlenirken hata — messageId: {}, hata: {}",
                    messageId, e.getMessage(), e);
        }
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
