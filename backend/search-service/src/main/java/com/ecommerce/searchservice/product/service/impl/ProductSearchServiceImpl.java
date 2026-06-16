package com.ecommerce.searchservice.product.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.query.AutocompleteSuggestionInfo;
import com.ecommerce.searchservice.product.query.BrandFacet;
import com.ecommerce.searchservice.product.query.ProductAvailabilityInfo;
import com.ecommerce.searchservice.product.service.ProductSearchService;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public ProductAvailabilityInfo getAvailability(String id) {
        // Kart ile aynı ES kaynağı → detayla tutarlı. Doküman yoksa (henüz indekslenmemiş/parent değil)
        // satışı bloklamamak için inStock=true varsay.
        ProductDocument doc = elasticsearchOperations.get(id, ProductDocument.class);
        if (doc == null) {
            return new ProductAvailabilityInfo(true, null);
        }
        return new ProductAvailabilityInfo(doc.isInStock(), doc.getSalesStatus());
    }

    @Override
    public Page<ProductDocument> searchProducts(ProductSearchRequest request) {
        log.info("Arama başlatıldı. Kriterler: {}", request);

        BoolQuery.Builder boolQueryBuilder = buildBaseQuery(request, true);

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

    @Override
    public List<ProductDocument> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> stringIds = ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .toList();
        if (stringIds.isEmpty()) {
            return List.of();
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(QueryBuilders.ids(i -> i.values(stringIds)))
                .withPageable(PageRequest.of(0, stringIds.size()))
                .build();

        Map<String, ProductDocument> byId = elasticsearchOperations.search(query, ProductDocument.class)
                .getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toMap(ProductDocument::getId, d -> d, (a, b) -> a));

        // Giriş sırasını koru (son gezilen sırası önemli); index'te olmayan id'ler elenir.
        return stringIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<ProductDocument> findSimilar(Long productId, int size) {
        ProductDocument source = elasticsearchOperations.get(productId.toString(), ProductDocument.class);
        if (source == null) {
            return List.of();
        }
        int safeSize = (size <= 0 || size > 30) ? 10 : size;

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(QueryBuilders.term(t -> t.field("tenantActive").value(true)));
        if (source.getCategoryId() != null) {
            bool.filter(QueryBuilders.term(t -> t.field("categoryId").value(source.getCategoryId())));
        }
        bool.mustNot(QueryBuilders.ids(i -> i.values(List.of(productId.toString()))));
        if (source.getBrand() != null && !source.getBrand().isBlank()) {
            bool.should(QueryBuilders.term(t -> t.field("brand").value(source.getBrand())));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withSort(buildSort("popular"))
                .withPageable(PageRequest.of(0, safeSize))
                .build();
        return elasticsearchOperations.search(query, ProductDocument.class)
                .getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    @Override
    public List<ProductDocument> findRelated(List<Long> seedIds, List<Long> excludeIds, int size) {
        int safeSize = (size <= 0 || size > 30) ? 12 : size;
        List<ProductDocument> seeds = findByIds(seedIds == null ? List.of() : seedIds);

        Set<Long> categoryIds = seeds.stream()
                .map(ProductDocument::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> brands = seeds.stream()
                .map(ProductDocument::getBrand).filter(b -> b != null && !b.isBlank()).collect(Collectors.toSet());

        Set<String> exclude = new HashSet<>();
        if (seedIds != null) seedIds.forEach(id -> { if (id != null) exclude.add(id.toString()); });
        if (excludeIds != null) excludeIds.forEach(id -> { if (id != null) exclude.add(id.toString()); });

        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.filter(QueryBuilders.term(t -> t.field("tenantActive").value(true)));
        if (!exclude.isEmpty()) {
            bool.mustNot(QueryBuilders.ids(i -> i.values(new ArrayList<>(exclude))));
        }
        if (!categoryIds.isEmpty()) {
            List<FieldValue> vals = categoryIds.stream().map(FieldValue::of).toList();
            bool.should(QueryBuilders.terms(t -> t.field("categoryId").terms(tt -> tt.value(vals))));
        }
        if (!brands.isEmpty()) {
            List<FieldValue> vals = brands.stream().map(FieldValue::of).toList();
            bool.should(QueryBuilders.terms(t -> t.field("brand").terms(tt -> tt.value(vals))));
        }
        // Seed varsa en az bir ilgi boyutu (kategori/marka) eşleşsin; seed yoksa should boş → trending fallback.
        if (!categoryIds.isEmpty() || !brands.isEmpty()) {
            bool.minimumShouldMatch("1");
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(bool.build()._toQuery())
                .withSort(buildSort("popular"))
                .withPageable(PageRequest.of(0, safeSize))
                .build();
        return elasticsearchOperations.search(query, ProductDocument.class)
                .getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    @Override
    public List<BrandFacet> getBrandFacets(ProductSearchRequest request) {
        // Marka facet'i: mevcut filtre bağlamı (kategori/arama/fiyat/puan/stok) için markaları say.
        // Markanın kendi filtresi HARİÇ tutulur ki seçili markalar listeyi daraltmasın.
        BoolQuery.Builder boolQueryBuilder = buildBaseQuery(request, false);

        Aggregation brandAgg = Aggregation.of(a -> a.terms(t -> t.field("brand").size(100)));

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(boolQueryBuilder.build()._toQuery())
                .withAggregation("brands", brandAgg)
                .withPageable(PageRequest.of(0, 1))
                .build();

        SearchHits<ProductDocument> hits =
                elasticsearchOperations.search(nativeQuery, ProductDocument.class);

        if (!(hits.getAggregations() instanceof ElasticsearchAggregations aggregations)) {
            return List.of();
        }
        var brandAggregation = aggregations.get("brands");
        if (brandAggregation == null) {
            return List.of();
        }

        return brandAggregation.aggregation().getAggregate().sterms().buckets().array().stream()
                .map(bucket -> new BrandFacet(bucket.key().stringValue(), bucket.docCount()))
                .toList();
    }

    /**
     * Ortak filtre bloğu — hem arama hem marka-facet sorgusu kullanır.
     * @param includeBrandFilter false ise marka filtresi atlanır (facet bağlamı için).
     */
    private BoolQuery.Builder buildBaseQuery(ProductSearchRequest request, boolean includeBrandFilter) {
        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();

        // Sadece aktif ve doğrulanmış mağazaların ürünleri görünsün
        boolQueryBuilder.filter(
                QueryBuilders.term(t -> t.field("tenantActive").value(true))
        );

        // Belirli bir mağaza sayfası için tenantId filtresi
        if (request.tenantId() != null) {
            boolQueryBuilder.filter(
                    QueryBuilders.term(t -> t.field("tenantId").value(request.tenantId()))
            );
        }

        // Öne çıkan ürünler vitrini — yalnızca featured=true istenince filtrele
        if (request.featured() != null && request.featured()) {
            boolQueryBuilder.filter(
                    QueryBuilders.term(t -> t.field("isFeatured").value(true))
            );
        }

        // inStock filtresi — null gelirse filtre uygulanmaz, true gelirse sadece stokta olanlar
        if (request.inStock() != null && request.inStock()) {
            boolQueryBuilder.filter(
                    QueryBuilders.term(t -> t.field("inStock").value(true))
            );
            // salesStatus filtresi: ON_SALE olanlar veya alan henüz set edilmemiş eski dokümanlar
            boolQueryBuilder.filter(
                    QueryBuilders.bool(b -> b
                            .should(QueryBuilders.term(t -> t.field("salesStatus").value("ON_SALE")))
                            .should(QueryBuilders.bool(b2 -> b2
                                    .mustNot(QueryBuilders.exists(e -> e.field("salesStatus")))
                            ))
                            .minimumShouldMatch("1")
                    )
            );
        }

        // Keyword — name, tags, description'da arar; AUTO fuzziness ile tek harf hatası tolere edilir
        if (request.keyword() != null && !request.keyword().isBlank()) {
            boolQueryBuilder.must(QueryBuilders.multiMatch(m -> m
                    .fields("name^3", "description", "tags^2")
                    .query(request.keyword())
                    .fuzziness("AUTO")
                    .prefixLength(1)
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

        // Brand filtresi (facet sorgusunda atlanır)
        if (includeBrandFilter && request.brands() != null && !request.brands().isEmpty()) {
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

        // Minimum puan filtresi (ratingAverage >= minRating)
        if (request.minRating() != null) {
            boolQueryBuilder.filter(QueryBuilders.range(r -> r
                    .untyped(u -> u.field("ratingAverage")
                            .gte(co.elastic.clients.json.JsonData.of(request.minRating())))
            ));
        }

        return boolQueryBuilder;
    }

    @Override
    public List<AutocompleteSuggestionInfo> autocomplete(String q, int size) {
        if (q == null || q.isBlank() || q.length() < 2) return List.of();

        // name: phrase_prefix (tam prefix) + fuzzy match (typo toleransı) ikisi birden
        // brand: keyword field → prefix query (phrase_prefix çalışmaz keyword alanda)
        Query query = QueryBuilders.bool(outerBool -> outerBool
                .filter(QueryBuilders.term(t -> t.field("tenantActive").value(true)))
                .must(QueryBuilders.bool(b -> b
                        .should(QueryBuilders.matchPhrasePrefix(m -> m
                                .field("name")
                                .query(q)
                                .maxExpansions(10)
                        ))
                        .should(QueryBuilders.match(m -> m
                                .field("name")
                                .query(q)
                                .fuzziness("AUTO")
                                .prefixLength(1)
                        ))
                        .should(QueryBuilders.prefix(p -> p
                                .field("brand")
                                .value(q.toLowerCase())
                        ))
                        .minimumShouldMatch("1")
                ))
        );

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(0, Math.min(size, 10)))
                .build();

        return elasticsearchOperations
                .search(nativeQuery, ProductDocument.class)
                .getSearchHits()
                .stream()
                .map(hit -> {
                    ProductDocument d = hit.getContent();
                    return new AutocompleteSuggestionInfo(
                            d.getId(),
                            d.getName(),
                            d.getMainImageUrl(),
                            d.getPrice(),
                            d.getCurrency()
                    );
                })
                .toList();
    }

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSort(String sortBy) {
        return switch (sortBy) {
            case "price_asc" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Asc))));
            case "price_desc" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("price").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));
            case "popular" -> List.of(
                    // Önce en çok satan, eşitlikte en çok görüntülenen (popülerlik = satış + ilgi).
                    co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .field(f -> f.field("saleCount")
                                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                    .missing("_last"))),
                    co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .field(f -> f.field("viewCount")
                                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                    .missing("_last"))));
            case "rating" -> List.of(co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                    .field(f -> f.field("ratingAverage").order(co.elastic.clients.elasticsearch._types.SortOrder.Desc))));
            default -> List.of(
                    // inStock olanlar her zaman önce — stokta olmayan ürünler sona iner
                    co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .field(f -> f.field("inStock")
                                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                    .missing("_last"))),
                    // ikincil kriter: en yeni ürün; null olanlar (eski seed data) en sona
                    co.elastic.clients.elasticsearch._types.SortOptions.of(s -> s
                            .field(f -> f.field("createdAt")
                                    .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                                    .missing("_last"))));
        };
    }

}
