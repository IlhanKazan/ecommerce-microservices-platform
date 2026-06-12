package com.ecommerce.searchservice.product.consumer;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.ecommerce.common.event.constants.EventConstants;
import com.ecommerce.contracts.event.tenant.TenantStatusChangedEventPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Mağaza durum değişikliklerini (pause/resume/close) dinler ve o mağazaya ait tüm
 * ürün dokümanlarında {@code tenantActive} bayrağını günceller. Arama sorgusu
 * {@code tenantActive=true} ile filtrelediği için, mağaza duraklatılınca/kapanınca
 * ürünler aramadan/vitrinden düşer; yeniden açılınca geri gelir.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantEventConsumer {

    private final ObjectMapper objectMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    @KafkaListener(topics = EventConstants.AGGREGATE_TENANT, groupId = "search-service-group")
    public void consumeTenantEvents(
            String messagePayload,
            @Header(value = "message_type", required = false) String headerEventType) {

        log.info("Kafka'dan TENANT event'i yakalandı! Gelen Tip: {}", headerEventType);

        if (!EventConstants.EVENT_TENANT_STATUS_CHANGED.equals(headerEventType)) {
            return;
        }

        try {
            String unescapedJson = objectMapper.readValue(messagePayload, String.class);
            TenantStatusChangedEventPayload payload =
                    objectMapper.readValue(unescapedJson, TenantStatusChangedEventPayload.class);

            boolean active = "ACTIVE".equals(payload.status());
            applyTenantActive(payload.tenantId(), active);

            log.info("ES tenantActive güncellendi. TenantId: {}, status: {}, tenantActive: {}",
                    payload.tenantId(), payload.status(), active);

        } catch (Exception e) {
            log.error("TENANT event işlenirken hata: {}", e.getMessage(), e);
        }
    }

    private void applyTenantActive(Long tenantId, boolean active) {
        Query byTenant = QueryBuilders.term(t -> t.field("tenantId").value(tenantId));

        UpdateQuery updateQuery = UpdateQuery.builder(NativeQuery.builder().withQuery(byTenant).build())
                .withScriptType(ScriptType.INLINE)
                .withLang("painless")
                .withScript("ctx._source.tenantActive = params.active")
                .withParams(Map.<String, Object>of("active", active))
                .build();

        elasticsearchOperations.updateByQuery(updateQuery, IndexCoordinates.of("products"));
    }
}
