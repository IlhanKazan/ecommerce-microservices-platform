package com.ecommerce.searchservice.product.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.controller.dto.ProductCreatedEventPayload;
import com.ecommerce.searchservice.product.mapper.ProductSearchMapper;
import com.ecommerce.searchservice.product.repository.ProductSearchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final ProductSearchRepository searchRepository;
    private final ObjectMapper objectMapper;
    private final ProductSearchMapper productSearchMapper;

    @KafkaListener(topics = EventConstants.AGGREGATE_PRODUCT, groupId = "search-service-group")
    public void consumeProductCreatedEvent(String messagePayload) {
        log.info("Kafka'dan PRODUCT event'i yakalandı! Veri: {}", messagePayload);

        try {
            String unescapedJson = objectMapper.readValue(messagePayload, String.class);
            ProductCreatedEventPayload payload = objectMapper.readValue(unescapedJson, ProductCreatedEventPayload.class);
            ProductDocument document = productSearchMapper.toDocument(payload);
            searchRepository.save(document);
            log.info("Ürün başarıyla Elasticsearch'e indekslendi. ES ID: {}", document.getId());

        } catch (JsonProcessingException e) {
            log.error("Kafka'dan gelen mesaj JSON formatına dönüştürülemedi: {}", e.getMessage());
            // TODO [09.03.2026 06:05]: DLQ(Dead Letter Queue) eklenebilir
        } catch (Exception e) {
            log.error("Elasticsearch'e kaydederken beklenmeyen hata: {}", e.getMessage(), e);
        }
    }
}
