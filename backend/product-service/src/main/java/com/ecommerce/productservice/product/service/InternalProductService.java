package com.ecommerce.productservice.product.service;

import com.ecommerce.productservice.product.query.ProductValidationInfo;

public interface InternalProductService {
    ProductValidationInfo validateAndGetProduct(Long productId, Long tenantId);
    int reindexAllProducts();
    void updateAiReport(Long productId, String aiReviewReport);
}