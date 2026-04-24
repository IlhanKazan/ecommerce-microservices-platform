package com.ecommerce.usertenantservice.outbox.service.impl;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.common.exception.SystemException;
import com.ecommerce.contracts.event.tenant.TenantActivatedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantCreatedEventPayload;
import com.ecommerce.contracts.event.tenant.TenantPaymentFailedEventPayload;
import com.ecommerce.usertenantservice.outbox.entity.Outbox;
import com.ecommerce.usertenantservice.outbox.repository.OutboxRepository;
import com.ecommerce.usertenantservice.outbox.service.OutboxService;
import com.ecommerce.usertenantservice.tenant.entity.Tenant;
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
    public void publishTenantCreatedEvent(Tenant tenant) {
        try {
            TenantCreatedEventPayload payload = new TenantCreatedEventPayload(
                    tenant.getId(),
                    tenant.getName(),
                    tenant.getContactEmail(),
                    tenant.getStatus().name()
            );

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_TENANT)
                    .aggregateId(tenant.getId().toString())
                    .messageType(EventConstants.EVENT_TENANT_CREATED)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: TENANT_CREATED_EVENT - Tenant ID: {}", tenant.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTenantActivatedEvent(Tenant tenant) {
        try {
            TenantActivatedEventPayload payload = new TenantActivatedEventPayload(
                    tenant.getId(),
                    tenant.getName(),
                    tenant.getContactEmail()
            );

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_TENANT)
                    .aggregateId(tenant.getId().toString())
                    .messageType(EventConstants.EVENT_TENANT_ACTIVATED)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: TENANT_ACTIVATED_EVENT - Tenant ID: {}", tenant.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishTenantPaymentFailedEvent(Tenant tenant) {
        try {
            TenantPaymentFailedEventPayload payload = new TenantPaymentFailedEventPayload(
                    tenant.getId(),
                    tenant.getName(),
                    tenant.getContactEmail()
            );

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_TENANT)
                    .aggregateId(tenant.getId().toString())
                    .messageType(EventConstants.EVENT_TENANT_PAYMENT_FAILED)
                    .messagePayload(objectMapper.writeValueAsString(payload))
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: TENANT_PAYMENT_FAILED_EVENT - Tenant ID: {}", tenant.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }
}