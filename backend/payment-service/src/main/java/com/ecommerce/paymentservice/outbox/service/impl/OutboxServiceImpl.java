package com.ecommerce.paymentservice.outbox.service.impl;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.common.exception.SystemException;
import com.ecommerce.contracts.event.payment.PaymentFailedEventPayload;
import com.ecommerce.contracts.event.payment.PaymentSuccessEventPayload;
import com.ecommerce.contracts.event.payment.SubscriptionActivatedEventPayload;
import com.ecommerce.paymentservice.outbox.entity.Outbox;
import com.ecommerce.paymentservice.outbox.repository.OutboxRepository;
import com.ecommerce.paymentservice.outbox.service.OutboxService;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.subscription.entity.TenantSubscription;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxServiceImpl implements OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPaymentSuccessEvent(Payment payment) {
        try {
            PaymentSuccessEventPayload payload = new PaymentSuccessEventPayload(
                    payment.getId(),
                    payment.getTenantId(),
                    payment.getCustomerId(),
                    payment.getOrderId(),
                    payment.getSubscriptionId(),
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getPaymentType().name()
            );

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_PAYMENT)
                    .aggregateId(payment.getId().toString())
                    .messageType(EventConstants.EVENT_PAYMENT_SUCCESS)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: PAYMENT_SUCCESS_EVENT - Payment ID: {}", payment.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishPaymentFailedEvent(Payment payment) {
        try {
            PaymentFailedEventPayload payload = new PaymentFailedEventPayload(
                    payment.getId(),
                    payment.getTenantId(),
                    payment.getCustomerId(),
                    payment.getOrderId(),
                    payment.getSubscriptionId(),
                    payment.getAmount(),
                    payment.getPaymentType().name(),
                    payment.getFailureReason(),
                    payment.getFailureCode()
            );

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_PAYMENT)
                    .aggregateId(payment.getId().toString())
                    .messageType(EventConstants.EVENT_PAYMENT_FAILED)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: PAYMENT_FAILED_EVENT - Payment ID: {}", payment.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishSubscriptionActivatedEvent(TenantSubscription subscription) {
        try {
            SubscriptionActivatedEventPayload payload = new SubscriptionActivatedEventPayload(
                    subscription.getId(),
                    subscription.getTenantId(),
                    subscription.getPlanName(),
                    subscription.getNextBillingDate()
            );

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_PAYMENT)
                    .aggregateId(subscription.getId().toString())
                    .messageType(EventConstants.EVENT_SUBSCRIPTION_ACTIVATED)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: SUBSCRIPTION_ACTIVATED_EVENT - Subscription ID: {}", subscription.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }
}
