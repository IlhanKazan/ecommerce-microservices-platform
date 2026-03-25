package com.ecommerce.stockservice.client.dto;

import com.ecommerce.stockservice.client.constants.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductResponse(
        Long id,
        String sku,
        ProductStatus status
) {}