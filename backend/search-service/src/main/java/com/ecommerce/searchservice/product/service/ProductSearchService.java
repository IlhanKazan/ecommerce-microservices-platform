package com.ecommerce.searchservice.product.service;

import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.document.ProductDocument;
import org.springframework.data.domain.Page;

public interface ProductSearchService {
    Page<ProductDocument> searchProducts(ProductSearchRequest request);
}
