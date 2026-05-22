package com.ecommerce.searchservice.product.service;

import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.query.AutocompleteSuggestionInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductSearchService {
    Page<ProductDocument> searchProducts(ProductSearchRequest request);
    List<AutocompleteSuggestionInfo> autocomplete(String q, int size);
}
