package com.ecommerce.searchservice.product.query;

import java.io.Serializable;
import java.math.BigDecimal;

public record AutocompleteSuggestionInfo(
        String id,
        String name,
        String mainImageUrl,
        BigDecimal price,
        String currency
) implements Serializable {}
