package com.ecommerce.searchservice.product.controller.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchRequest(
        String keyword,
        List<Long> categoryIds,
        List<String> brands,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock,
        String sortBy,
        int page,
        int size
) {
    public ProductSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 20;
        if (sortBy == null) sortBy = "newest";
    }
}
