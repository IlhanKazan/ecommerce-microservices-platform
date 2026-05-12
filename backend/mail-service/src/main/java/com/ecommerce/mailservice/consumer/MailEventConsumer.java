package com.ecommerce.mailservice.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.contracts.event.tenant.TenantActivatedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantPaymentFailedEventPayload;
import com.ecommerce.mailservice.inbox.service.InboxService;
import com.ecommerce.mailservice.mail.handler.TenantMailHandler;
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
public class MailEventConsumer {

    private final InboxService inboxService;
    private final TenantMailHandler tenantMailHandler;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = EventConstants.AGGREGATE_TENANT, groupId = "mail-service-group")
    public void consumeTenantEvents(ConsumerRecord<String, String> record) {
        String eventType = extractHeader(record, "message_type");
        // Benzersiz messageId: topic + partition + offset — Debezium'da header'da ID yok
        String messageId = record.topic() + ":" + record.partition() + ":" + record.offset();

        log.info("TENANT event alındı — eventType: {}, messageId: {}", eventType, messageId);

        try {
            String json = objectMapper.readValue(record.value(), String.class);

            if (inboxService.isAlreadyProcessed(messageId, eventType, json)) {
                return;
            }

            switch (eventType) {
                case EventConstants.EVENT_TENANT_ACTIVATED -> {
                    TenantActivatedEventPayload payload =
                            objectMapper.readValue(json, TenantActivatedEventPayload.class);
                    tenantMailHandler.handleTenantActivated(payload, messageId);
                }
                case EventConstants.EVENT_TENANT_PAYMENT_FAILED -> {
                    TenantPaymentFailedEventPayload payload =
                            objectMapper.readValue(json, TenantPaymentFailedEventPayload.class);
                    tenantMailHandler.handleTenantPaymentFailed(payload, messageId);
                }
                case EventConstants.EVENT_TENANT_CREATED ->
                        log.debug("TENANT_CREATED event mail-service'te işlenmiyor — atlandı");
                case null, default ->
                        log.warn("Bilinmeyen TENANT event tipi: {}", eventType);
            }

        } catch (Exception e) {
            log.error("TENANT event işlenirken hata — eventType: {}, messageId: {}, hata: {}",
                    eventType, messageId, e.getMessage(), e);
            // Exception fırlatılmıyor — Kafka'da sonsuz retry'a düşmesin.
            // Kritik teslimat gerekliyse DLQ publish eklenebilir.
        }
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    // TODO: PAYMENT topic consumer — PaymentSuccessEventPayload'da alıcı email yok.
    // Çözüm: (A) PaymentSuccessEventPayload'a recipientEmail ekle (additive)
    //         (B) UTS Feign ile tenantId → contactEmail resolve et.
    // MVP scope'u dışında, sonraki iterasyonda eklenecek.
}
