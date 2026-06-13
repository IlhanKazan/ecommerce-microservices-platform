package com.ecommerce.searchservice.product.controller;

import com.ecommerce.searchservice.common.constants.ApiPaths;
import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.query.AutocompleteSuggestionInfo;
import com.ecommerce.searchservice.product.query.BrandFacet;
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
}