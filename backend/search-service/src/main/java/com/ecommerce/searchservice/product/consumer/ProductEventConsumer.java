package com.ecommerce.searchservice.product.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.searchservice.product.consumer.dto.InventoryEventPayload;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.consumer.dto.ProductCreatedEventPayload;
import com.ecommerce.searchservice.product.mapper.ProductSearchMapper;
import com.ecommerce.searchservice.product.repository.ProductSearchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final ProductSearchRepository searchRepository;
    private final ObjectMapper objectMapper;
    private final ProductSearchMapper productSearchMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    @KafkaListener(topics = EventConstants.AGGREGATE_PRODUCT, groupId = "search-service-group")
    public void consumeProductCreatedEvent(String messagePayload) {
        log.info("Kafka'dan PRODUCT event'i yakalandı! Veri: {}", messagePayload);

        try {
            String unescapedJson = objectMapper.readValue(messagePayload, String.class);
            ProductCreatedEventPayload payload = objectMapper.readValue(unescapedJson, ProductCreatedEventPayload.class);
            ProductDocument document = productSearchMapper.toDocument(payload);

            // Manual stok eklenene kadar
            document.setInStock(false);

            searchRepository.save(document);
            log.info("Ürün başarıyla Elasticsearch'e indekslendi. ES ID: {}", document.getId());

        } catch (JsonProcessingException e) {
            log.error("Kafka'dan gelen mesaj JSON formatına dönüştürülemedi: {}", e.getMessage());
            // TODO [09.03.2026 06:05]: DLQ(Dead Letter Queue) eklenebilir
        } catch (Exception e) {
            log.error("Elasticsearch'e kaydederken beklenmeyen hata: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = EventConstants.AGGREGATE_STOCK, groupId = "search-service-group")
    public void consumeInventoryEvent(String messagePayload) {
        log.info("Kafka'dan INVENTORY event'i yakalandı!");

        try {
            String unescapedJson = objectMapper.readValue(messagePayload, String.class);
            // Tüm JSON'ı objeye çevirmeden önce sadece ağaç yapısını oku
            JsonNode rootNode = objectMapper.readTree(unescapedJson);
            String eventType = rootNode.has("eventType") ? rootNode.get("eventType").asText() : "";

            // Eğer event tipi STOCK_STATUS_CHANGED değilse eventi görmezden gel
            if (!EventConstants.EVENT_STOCK_STATUS_CHANGED.equals(eventType)) {
                return;
            }

            // Sadece bizi ilgilendiren event ise DTO'ya çevir ve Elasticsearch'ü güncelle
            InventoryEventPayload payload = objectMapper.treeToValue(rootNode, InventoryEventPayload.class);

            if (payload.inStock() != null) {
                Document document = Document.create();
                document.put("inStock", payload.inStock());

                UpdateQuery updateQuery = UpdateQuery.builder(payload.productId().toString())
                        .withDocument(document)
                        .build();

                elasticsearchOperations.update(updateQuery, IndexCoordinates.of("products"));
                log.info("ES Güncellendi: Product ID: {}, inStock: {}", payload.productId(), payload.inStock());
            }

        } catch (Exception e) {
            log.error("Elasticsearch INVENTORY güncelleme hatası: {}", e.getMessage(), e);
        }
    }
}
