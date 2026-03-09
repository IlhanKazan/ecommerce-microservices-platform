package com.ecommerce.searchservice.product.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchRequest(
        String keyword,
        Long categoryId,
        List<String> brands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int page,
        int size
) {
    public ProductSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
    }
}
