package com.ecommerce.searchservice.product.controller;

import com.ecommerce.searchservice.common.constants.ApiPaths;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.service.ProductSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.PublicProduct.PUBLIC_SEARCH_PRODUCTS)
@RequiredArgsConstructor
public class PublicProductSearchController {

    private final ProductSearchService searchService;

    // Filtreler uzun olabileceği için GET yerine POST kullanılıyor
    @PostMapping
    public ResponseEntity<Page<ProductDocument>> searchProducts(@RequestBody ProductSearchRequest request) {
        Page<ProductDocument> result = searchService.searchProducts(request);
        return ResponseEntity.ok(result);
    }
}