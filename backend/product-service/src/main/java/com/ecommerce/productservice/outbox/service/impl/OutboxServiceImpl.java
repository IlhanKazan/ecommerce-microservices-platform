package com.ecommerce.productservice.outbox.service.impl;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.common.exception.SystemException;
import com.ecommerce.productservice.outbox.entity.Outbox;
import com.ecommerce.productservice.outbox.payload.ProductCreatedEventPayload;
import com.ecommerce.productservice.outbox.payload.ProductDeletedEventPayload;
import com.ecommerce.productservice.outbox.payload.ProductUpdatedEventPayload;
import com.ecommerce.productservice.outbox.repository.OutboxRepository;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.entity.Product;
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

    // Mevcut transaction'a dahil olmak zorundayız, ürün kaydı rollback olursa bu da olmalı.
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishProductCreatedEvent(Product product) {
        try {
            var payload = new ProductCreatedEventPayload(
                    product.getId(),
                    product.getTenantId(),
                    product.getCategory().getId(),
                    product.getSku(),
                    product.getName(),
                    product.getDescription(),
                    product.getBrand(),
                    product.getPrice(),
                    product.getCurrency(),
                    product.getMainImageUrl(),
                    product.getAttributes(),
                    product.getTags()
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_PRODUCT)
                    .aggregateId(product.getId().toString())
                    .messageType(EventConstants.EVENT_PRODUCT_CREATED)
                    .messagePayload(jsonPayload)
                    .processed(false)
                    .build();

            outboxRepository.save(outbox);
            log.info("Outbox kaydı oluşturuldu: Ürün ID {}", product.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCCESSING_ERROR");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishProductUpdatedEvent(Product product) {
        try {
            var payload = new ProductUpdatedEventPayload(
                    product.getId(),
                    product.getTenantId(),
                    product.getCategory().getId(),
                    product.getSku(),
                    product.getName(),
                    product.getDescription(),
                    product.getBrand(),
                    product.getPrice(),
                    product.getCurrency(),
                    product.getMainImageUrl(),
                    product.getAttributes(),
                    product.getTags(),
                    product.getStatus().name(),
                    product.getSalesStatus().name()
            );

            String jsonPayload = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_PRODUCT)
                    .aggregateId(product.getId().toString())
                    .messageType(EventConstants.EVENT_PRODUCT_UPDATED)
                    .messagePayload(jsonPayload)
                    .processed(false)
                    .build();

            outboxRepository.save(outbox);
            log.info("Outbox güncellendi kaydı oluşturuldu: Ürün ID {}", product.getId());

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası (Update): {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCCESSING_ERROR");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishProductDeletedEvent(Long productId, Long tenantId) {
        try {
            var payload = new ProductDeletedEventPayload(productId, tenantId);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            Outbox outbox = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_PRODUCT)
                    .aggregateId(productId.toString())
                    .messageType(EventConstants.EVENT_PRODUCT_DELETED)
                    .messagePayload(jsonPayload)
                    .processed(false)
                    .build();

            outboxRepository.save(outbox);
            log.info("Outbox silindi kaydı oluşturuldu: Ürün ID {}", productId);

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası (Delete): {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCCESSING_ERROR");
        }
    }

}
