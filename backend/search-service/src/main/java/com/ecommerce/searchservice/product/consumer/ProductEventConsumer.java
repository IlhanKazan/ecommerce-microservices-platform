package com.ecommerce.searchservice.product.consumer;

import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.contracts.event.product.ProductCreatedEventPayload;
import com.ecommerce.contracts.event.product.ProductDeletedEventPayload;
import com.ecommerce.contracts.event.product.ProductUpdatedEventPayload;
import com.ecommerce.contracts.event.stock.StockStatusChangedEventPayload;
import com.ecommerce.searchservice.client.adapter.UserTenantClientAdapter;
import com.ecommerce.searchservice.client.dto.TenantStorefrontResponse;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.mapper.ProductSearchMapper;
import com.ecommerce.searchservice.product.repository.ProductSearchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEventConsumer {

    private final ProductSearchRepository searchRepository;
    private final ObjectMapper objectMapper;
    private final ProductSearchMapper productSearchMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final UserTenantClientAdapter userTenantClientAdapter;

    @KafkaListener(topics = EventConstants.AGGREGATE_PRODUCT, groupId = "search-service-group")
    public void consumeProductEvents(
            String messagePayload,
            @Header(value = "message_type", required = false) String headerEventType) {

        log.info("Kafka'dan PRODUCT event'i yakalandı! Gelen Tip: {}", headerEventType);

        try {
            String unescapedJson = objectMapper.readValue(messagePayload, String.class);

            switch (headerEventType) {
                case EventConstants.EVENT_PRODUCT_CREATED -> handleProductCreated(unescapedJson);
                case EventConstants.EVENT_PRODUCT_UPDATED -> handleProductUpdated(unescapedJson);
                case EventConstants.EVENT_PRODUCT_DELETED -> handleProductDeleted(unescapedJson);
                case null, default -> log.warn("Bilinmeyen bir PRODUCT event tipi geldi: {}", headerEventType);
            }

        } catch (Exception e) {
            log.error("PRODUCT event işlenirken beklenmeyen hata: {}", e.getMessage(), e);
        }
    }

    private void handleProductCreated(String json) throws JsonProcessingException {
        ProductCreatedEventPayload payload = objectMapper.readValue(json, ProductCreatedEventPayload.class);
        ProductDocument document = productSearchMapper.toDocument(payload);
        document.setInStock(false);
        document.setCreatedAt(LocalDateTime.now());
        enrichWithTenantInfo(document, payload.tenantId());
        searchRepository.save(document);
        log.info("ES Yeni Ürün İndekslendi: {}", document.getId());
    }

    private void handleProductUpdated(String json) throws JsonProcessingException {
        ProductUpdatedEventPayload payload = objectMapper.readValue(json, ProductUpdatedEventPayload.class);

        boolean isNew = !searchRepository.existsById(payload.productId().toString());
        ProductDocument document = searchRepository.findById(payload.productId().toString())
                .orElse(new ProductDocument());

        if (isNew) {
            document.setCreatedAt(LocalDateTime.now());
        }
        document.setId(payload.productId().toString());
        document.setTenantId(payload.tenantId());
        document.setCategoryId(payload.categoryId());
        document.setSku(payload.sku());
        document.setName(payload.name());
        document.setDescription(payload.description());
        document.setBrand(payload.brand());
        document.setPrice(payload.price());
        document.setDiscountedPrice(payload.discountedPrice());
        document.setCurrency(payload.currency());
        document.setMainImageUrl(payload.mainImageUrl());
        document.setAttributes(payload.attributes());
        document.setTags(payload.tags());

        // inStock'a dokunma — gerçek fiziksel stok durumunu yalnızca stock-service event'leri yönetir.
        // salesStatus'ü ayrı tut; search query her iki alanı birlikte filtreler.
        document.setSalesStatus(payload.salesStatus());

        if (payload.ratingAverage() != null) {
            document.setRatingAverage(payload.ratingAverage().doubleValue());
        }
        if (payload.reviewCount() != null) {
            document.setReviewCount(payload.reviewCount());
        }

        enrichWithTenantInfo(document, payload.tenantId());
        searchRepository.save(document);
        log.info("ES Ürün Güncellendi. ID: {}, salesStatus: {}", payload.productId(), payload.salesStatus());
    }

    private void enrichWithTenantInfo(ProductDocument document, Long tenantId) {
        TenantStorefrontResponse storefront = userTenantClientAdapter.getStorefront(tenantId);
        if (storefront != null) {
            document.setTenantName(storefront.name());
            document.setTenantLogoUrl(storefront.logoUrl());
            document.setTenantActive(
                "ACTIVE".equals(storefront.status()) && Boolean.TRUE.equals(storefront.isVerified())
            );
        } else {
            document.setTenantActive(false);
        }
    }

    private void handleProductDeleted(String json) throws JsonProcessingException {
        ProductDeletedEventPayload payload = objectMapper.readValue(json, ProductDeletedEventPayload.class);
        searchRepository.deleteById(payload.productId().toString());
        log.info("ES Ürün Tamamen Silindi. ID: {}", payload.productId());
    }

    @KafkaListener(topics = EventConstants.AGGREGATE_STOCK, groupId = "search-service-group")
    public void consumeInventoryEvent(
            String messagePayload,
            @Header(value = "message_type", required = false) String headerEventType) {

        log.info("Kafka'dan STOCK event'i yakalandı! Gelen Tip: {}", headerEventType);

        if (!EventConstants.EVENT_STOCK_STATUS_CHANGED.equals(headerEventType)) {
            return;
        }

        try {
            String unescapedJson = objectMapper.readValue(messagePayload, String.class);
            StockStatusChangedEventPayload payload =
                    objectMapper.readValue(unescapedJson, StockStatusChangedEventPayload.class);

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
            log.error("Elasticsearch STOCK güncelleme hatası: {}", e.getMessage(), e);
        }
    }
}