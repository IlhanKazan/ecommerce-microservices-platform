package com.ecommerce.searchservice.product.controller;

import com.ecommerce.searchservice.common.constants.ApiPaths;
import com.ecommerce.searchservice.product.controller.dto.ProductIdsRequest;
import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.controller.dto.RelatedRequest;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.query.AutocompleteSuggestionInfo;
import com.ecommerce.searchservice.product.query.BrandFacet;
import com.ecommerce.searchservice.product.query.ProductAvailabilityInfo;
import com.ecommerce.searchservice.product.service.ProductSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Search", description = "Elasticsearch-backed full-text product search and autocomplete — no authentication required for all endpoints")
@RestController
@RequestMapping(ApiPaths.PublicProduct.BASE)
@RequiredArgsConstructor
public class PublicProductSearchController {

    private final ProductSearchService searchService;

    @Operation(
        summary = "Search products",
        description = "Full-text search over the product catalog with optional filters. Backed by Elasticsearch. Results are paginated and sorted by relevance.",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "Search results page — each hit contains product name, price, images, tenant, inStock flag")
    @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    // Filtreler uzun olabileceği için GET yerine POST kullanılıyor
    @PostMapping(ApiPaths.PublicProduct.PRODUCTS)
    public ResponseEntity<Page<ProductDocument>> searchProducts(@RequestBody ProductSearchRequest request) {
        Page<ProductDocument> result = searchService.searchProducts(request);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Hydrate products by ids",
        description = "Id listesiyle ürün kartlarını getirir, giriş sırasını koruyarak. Son gezilenler / birlikte alınanlar gibi rail'lerin hidrasyonu için.",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "Verilen id'lere ait ürünler (sırayla)")
    @PostMapping(ApiPaths.PublicProduct.PRODUCTS_BY_IDS)
    public ResponseEntity<List<ProductDocument>> getProductsByIds(@RequestBody ProductIdsRequest request) {
        return ResponseEntity.ok(searchService.findByIds(request.ids()));
    }

    @Operation(
        summary = "Product name autocomplete",
        description = "Returns up to N product name suggestions matching the query prefix. Designed for search bar real-time suggestions (300ms debounce recommended on client).",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "List of autocomplete suggestions with product name and image URL")
    @GetMapping(ApiPaths.PublicProduct.AUTOCOMPLETE)
    public ResponseEntity<List<AutocompleteSuggestionInfo>> autocomplete(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(searchService.autocomplete(q, size));
    }

    @Operation(
        summary = "Brand facets",
        description = "Mevcut filtre bağlamındaki (kategori/arama/fiyat/puan/stok) markaları ürün sayısıyla döner. Marka filtresi panelini doldurmak için.",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "Marka + ürün sayısı listesi")
    @PostMapping(ApiPaths.PublicProduct.BRANDS)
    public ResponseEntity<List<BrandFacet>> getBrands(@RequestBody ProductSearchRequest request) {
        return ResponseEntity.ok(searchService.getBrandFacets(request));
    }

    @Operation(
        summary = "Similar products",
        description = "Bir ürüne benzer ürünler (içerik-tabanlı): aynı kategori + aynı marka boost, popülerliğe göre, kendisi hariç. Ürün detayı 'Benzer ürünler' rail'i için.",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "Benzer ürün listesi")
    @GetMapping(ApiPaths.PublicProduct.SIMILAR)
    public ResponseEntity<List<ProductDocument>> similar(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchService.findSimilar(productId, size));
    }

    @Operation(
        summary = "Related products (seed-based)",
        description = "Verilen tohum ürünlerin (son gezilen + favori) kategori/marka'sına göre ilgili ürünler. 'Senin için önerilenler' rail'inin içerik-tabanlı fallback'i; AI Engine sonradan bu yolu devralabilir.",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "İlgili ürün listesi")
    @PostMapping(ApiPaths.PublicProduct.RELATED)
    public ResponseEntity<List<ProductDocument>> related(@RequestBody RelatedRequest request) {
        int size = request.size() != null ? request.size() : 12;
        return ResponseEntity.ok(searchService.findRelated(request.seedIds(), request.excludeIds(), size));
    }

    @Operation(
        summary = "Product availability",
        description = "Tek ürünün fiziksel stok durumu (ES inStock + salesStatus). Detay sayfası 'Tükendi' göstergesi için — kartla aynı kaynak.",
        security = {}
    )
    @ApiResponse(responseCode = "200", description = "inStock + salesStatus")
    @GetMapping(ApiPaths.PublicProduct.AVAILABILITY)
    public ResponseEntity<ProductAvailabilityInfo> getAvailability(@PathVariable String id) {
        return ResponseEntity.ok(searchService.getAvailability(id));
    }
}