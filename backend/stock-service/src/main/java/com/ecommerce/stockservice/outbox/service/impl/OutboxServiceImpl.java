package com.ecommerce.stockservice.outbox.service.impl;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.common.exception.SystemException;
import com.ecommerce.stockservice.outbox.entity.Outbox;
import com.ecommerce.stockservice.outbox.payload.StockReservedPayload;
import com.ecommerce.stockservice.outbox.payload.StockStatusChangedPayload;
import com.ecommerce.stockservice.outbox.repository.OutboxRepository;
import com.ecommerce.stockservice.outbox.service.OutboxService;
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

    // Sadece Transaction olan bir yerden çağrılabilir
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishStockStatusChangedEvent(String aggregateId, Long productId, boolean inStock, String status) {
        try {
            StockStatusChangedPayload payload = new StockStatusChangedPayload(productId, inStock, status);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_STOCK)
                    .aggregateId(aggregateId)
                    .eventType(EventConstants.EVENT_STOCK_STATUS_CHANGED)
                    .messagePayload(jsonPayload)
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Outbox kaydı oluşturuldu: Product ID {} - inStock: {}", productId, inStock);

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }

    // SAGA'nın ilk adımı için fırlatılacak event
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishStockReservedEvent(String aggregateId, Long productId, int amount, String orderId) {
        try {
            StockReservedPayload payload = new StockReservedPayload(
                    orderId, productId, amount, "RESERVED", EventConstants.EVENT_STOCK_RESERVED
            );
            String jsonPayload = objectMapper.writeValueAsString(payload);

            Outbox outboxEvent = Outbox.builder()
                    .aggregateType(EventConstants.AGGREGATE_STOCK)
                    .aggregateId(aggregateId)
                    .eventType(EventConstants.EVENT_STOCK_RESERVED)
                    .messagePayload(jsonPayload)
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("SAGA Eventi: Stok başarıyla rezerve edildi. OrderID: {}", orderId);

        } catch (JsonProcessingException e) {
            log.error("Outbox payload JSON çevrim hatası: {}", e.getMessage());
            throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
        }
    }

}
