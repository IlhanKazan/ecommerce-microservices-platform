package com.ecommerce.searchservice.product.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Product search request with full-text query and optional filters")
public record ProductSearchRequest(
        @Schema(description = "Full-text search query", example = "blue running shoes")
        String keyword,
        @Schema(description = "Filter by category ID", example = "5")
        List<Long> categoryIds,
        List<String> brands,
        @Schema(description = "Minimum price filter in TRY", example = "100.00")
        BigDecimal minPrice,
        @Schema(description = "Maximum price filter in TRY", example = "500.00")
        BigDecimal maxPrice,
        Boolean inStock,
        String sortBy,
        @Schema(description = "Filter by tenant (store) ID", example = "42")
        Long tenantId,
        @Schema(description = "Page number (0-indexed)", example = "0")
        int page,
        @Schema(description = "Page size (max 50)", example = "20")
        int size
) {
    public ProductSearchRequest {
        if (page < 0) page = 0;
        if (size <= 0 || size > 50) size = 20;
        if (sortBy == null) sortBy = "newest";
    }
}
