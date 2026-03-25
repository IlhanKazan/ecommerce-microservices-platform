package com.ecommerce.searchservice.product.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Page<ProductDocument> searchProducts(ProductSearchRequest request) {
        log.info("Arama başlatıldı. Kriterler: {}", request);

        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();

        // Müşteriye sadece stokta olan ürünleri gösterir
        boolQueryBuilder.filter(QueryBuilders.term(t -> t.field("inStock").value(true)));

        if (request.keyword() != null && !request.keyword().isBlank()) {
            Query multiMatchQuery = QueryBuilders.multiMatch(m -> m
                    .fields("name^3", "description", "tags^2")
                    .query(request.keyword())
            );
            boolQueryBuilder.must(multiMatchQuery);
        }

        if (request.categoryId() != null) {
            boolQueryBuilder.filter(QueryBuilders.term(t -> t.field("categoryId").value(request.categoryId())));
        }

        if (request.brands() != null && !request.brands().isEmpty()) {
            List<FieldValue> brandValues = request.brands().stream()
                    .map(FieldValue::of)
                    .toList();
            boolQueryBuilder.filter(QueryBuilders.terms(t -> t.field("brand").terms(terms -> terms.value(brandValues))));
        }

        if (request.minPrice() != null || request.maxPrice() != null) {
            boolQueryBuilder.filter(QueryBuilders.range(r -> r
                    .untyped( u -> u
                        .field("price")
                        .gte(request.minPrice() != null ? co.elastic.clients.json.JsonData.of(request.minPrice()) : null)
                        .lte(request.maxPrice() != null ? co.elastic.clients.json.JsonData.of(request.maxPrice()) : null)
                    )
            ));
        }

        Pageable pageable = PageRequest.of(request.page(), request.size());

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQueryBuilder.build()._toQuery())
                .withPageable(pageable)
                // TODO [09.03.2026 07:02]: İleride buraya .withSort() ekleyip fiyata göre sıralama
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<ProductDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(documents, pageable, searchHits.getTotalHits());
    }

}
