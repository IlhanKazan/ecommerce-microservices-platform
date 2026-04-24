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

        // inStock filtresi — null gelirse filtre uygulanmaz, true gelirse sadece stokta olanlar
        if (request.inStock() != null && request.inStock()) {
            boolQueryBuilder.filter(
                    QueryBuilders.term(t -> t.field("inStock").value(true))
            );
        }

        // Keyword — name, tags, description'da arar
        if (request.keyword() != null && !request.keyword().isBlank()) {
            boolQueryBuilder.must(QueryBuilders.multiMatch(m -> m
                    .fields("name^3", "description", "tags^2")
                    .query(request.keyword())
            ));
        }

        // Category — frontend tüm alt kategori ID'lerini zaten biliyor, terms filter
        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            List<FieldValue> categoryValues = request.categoryIds().stream()
                    .map(FieldValue::of)
                    .toList();
            boolQueryBuilder.filter(
                    QueryBuilders.terms(t -> t.field("categoryId")
                            .terms(terms -> terms.value(categoryValues)))
            );
        }

        // Brand filtresi
        if (request.brands() != null && !request.brands().isEmpty()) {
            List<FieldValue> brandValues = request.brands().stream()
                    .map(FieldValue::of)
                    .toList();
            boolQueryBuilder.filter(
                    QueryBuilders.terms(t -> t.field("brand")
                            .terms(terms -> terms.value(brandValues)))
            );
        }

        // Fiyat aralığı
        if (request.minPrice() != null || request.maxPrice() != null) {
            boolQueryBuilder.filter(QueryBuilders.range(r -> r
                    .untyped(u -> {
                        u.field("price");
                        if (request.minPrice() != null)
                            u.gte(co.elastic.clients.json.JsonData.of(request.minPrice()));
                        if (request.maxPrice() != null)
                            u.lte(co.elastic.clients.json.JsonData.of(request.maxPrice()));
                        return u;
                    })
            ));
        }

        // Sort
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions =
                buildSort(request.sortBy());

        Pageable pageable = PageRequest.of(request.page(), request.size());

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQueryBuilder.build()._toQuery())
                .withSort(sortOptions)
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> searchHits =
                elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        List<ProductDocument> documents = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(documents, pageable, searchHits.getTotalHits());
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSort(String sortBy) {
        return switch (sortBy) {
            case "price_asc" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))));
            case "price_desc" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));
            case "popular" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("saleCount").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));
            case "rating" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("ratingAverage").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));
            default -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s  // newest
                    .field(f -> f.field("createdAt").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));
        };
    }

}
