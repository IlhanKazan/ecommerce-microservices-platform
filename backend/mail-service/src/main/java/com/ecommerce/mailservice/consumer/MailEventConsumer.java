package com.ecommerce.mailservice.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.contracts.event.order.OrderCancelledEventPayload;
import com.ecommerce.contracts.event.order.OrderConfirmedEventPayload;
import com.ecommerce.contracts.event.order.OrderDeliveredEventPayload;
import com.ecommerce.contracts.event.order.OrderRefundedEventPayload;
import com.ecommerce.contracts.event.order.OrderReturnRejectedEventPayload;
import com.ecommerce.contracts.event.order.OrderReturnRequestedEventPayload;
import com.ecommerce.contracts.event.order.OrderReturnedEventPayload;
import com.ecommerce.contracts.event.order.OrderShippedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionActivatedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionPlanChangedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionRenewalFailedEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionRenewalSuccessEventPayload;
import com.ecommerce.contracts.event.tenant.TenantActivatedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantPaymentFailedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantStatusChangedEventPayload;
import com.ecommerce.mailservice.inbox.service.InboxService;
import com.ecommerce.mailservice.mail.handler.OrderMailHandler;
import com.ecommerce.mailservice.mail.handler.SubscriptionMailHandler;
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
    private final OrderMailHandler orderMailHandler;
    private final SubscriptionMailHandler subscriptionMailHandler;
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
                case EventConstants.EVENT_TENANT_STATUS_CHANGED -> {
                    TenantStatusChangedEventPayload payload =
                            objectMapper.readValue(json, TenantStatusChangedEventPayload.class);
                    tenantMailHandler.handleTenantStatusChanged(payload, messageId);
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

    @KafkaListener(topics = EventConstants.AGGREGATE_ORDER, groupId = "mail-service-group")
    public void consumeOrderEvents(ConsumerRecord<String, String> record) {
        String eventType = extractHeader(record, "message_type");
        String messageId = record.topic() + ":" + record.partition() + ":" + record.offset();

        log.info("ORDER event alındı — eventType: {}, messageId: {}", eventType, messageId);

        try {
            String json = objectMapper.readValue(record.value(), String.class);

            if (inboxService.isAlreadyProcessed(messageId, eventType, json)) {
                return;
            }

            switch (eventType) {
                case EventConstants.EVENT_ORDER_CONFIRMED -> {
                    OrderConfirmedEventPayload payload =
                            objectMapper.readValue(json, OrderConfirmedEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderConfirmed(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_CANCELLED -> {
                    OrderCancelledEventPayload payload =
                            objectMapper.readValue(json, OrderCancelledEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderCancelled(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_SHIPPED -> {
                    OrderShippedEventPayload payload =
                            objectMapper.readValue(json, OrderShippedEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderShipped(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_REFUNDED -> {
                    OrderRefundedEventPayload payload =
                            objectMapper.readValue(json, OrderRefundedEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderRefunded(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_DELIVERED -> {
                    OrderDeliveredEventPayload payload =
                            objectMapper.readValue(json, OrderDeliveredEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderDelivered(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_RETURN_REQUESTED -> {
                    OrderReturnRequestedEventPayload payload =
                            objectMapper.readValue(json, OrderReturnRequestedEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderReturnRequested(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_RETURN_REJECTED -> {
                    OrderReturnRejectedEventPayload payload =
                            objectMapper.readValue(json, OrderReturnRejectedEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderReturnRejected(payload, messageId);
                    }
                }
                case EventConstants.EVENT_ORDER_RETURNED -> {
                    OrderReturnedEventPayload payload =
                            objectMapper.readValue(json, OrderReturnedEventPayload.class);
                    if (payload.recipientEmail() != null) {
                        orderMailHandler.handleOrderReturned(payload, messageId);
                    }
                }
                case null, default ->
                        log.debug("Mail-service için ORDER event ilgisiz: {}", eventType);
            }

        } catch (Exception e) {
            log.error("ORDER event işlenirken hata — eventType: {}, messageId: {}, hata: {}",
                    eventType, messageId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = EventConstants.AGGREGATE_PAYMENT, groupId = "mail-service-group")
    public void consumePaymentEvents(ConsumerRecord<String, String> record) {
        String eventType = extractHeader(record, "message_type");
        String messageId = record.topic() + ":" + record.partition() + ":" + record.offset();

        log.info("PAYMENT event alındı — eventType: {}, messageId: {}", eventType, messageId);

        try {
            String json = objectMapper.readValue(record.value(), String.class);

            if (inboxService.isAlreadyProcessed(messageId, eventType, json)) {
                return;
            }

            switch (eventType) {
                case EventConstants.EVENT_SUBSCRIPTION_ACTIVATED -> {
                    SubscriptionActivatedEventPayload payload =
                            objectMapper.readValue(json, SubscriptionActivatedEventPayload.class);
                    subscriptionMailHandler.handleSubscriptionActivated(payload, messageId);
                }
                case EventConstants.EVENT_SUBSCRIPTION_RENEWAL_SUCCESS -> {
                    SubscriptionRenewalSuccessEventPayload payload =
                            objectMapper.readValue(json, SubscriptionRenewalSuccessEventPayload.class);
                    subscriptionMailHandler.handleRenewalSuccess(payload, messageId);
                }
                case EventConstants.EVENT_SUBSCRIPTION_RENEWAL_FAILED -> {
                    SubscriptionRenewalFailedEventPayload payload =
                            objectMapper.readValue(json, SubscriptionRenewalFailedEventPayload.class);
                    subscriptionMailHandler.handleRenewalFailed(payload, messageId);
                }
                case EventConstants.EVENT_SUBSCRIPTION_PLAN_CHANGED -> {
                    SubscriptionPlanChangedEventPayload payload =
                            objectMapper.readValue(json, SubscriptionPlanChangedEventPayload.class);
                    subscriptionMailHandler.handlePlanChanged(payload, messageId);
                }
                case null, default ->
                        log.debug("Mail-service için PAYMENT event ilgisiz: {}", eventType);
            }

        } catch (Exception e) {
            log.error("PAYMENT event işlenirken hata — eventType: {}, messageId: {}, hata: {}",
                    eventType, messageId, e.getMessage(), e);
        }
    }

    private String extractHeader(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null) return null;
        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
