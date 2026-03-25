package com.ecommerce.basketservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductClientResponse(
        Long id,
        String name,
        BigDecimal price,
        String mainImageUrl,
        String status,
        String salesStatus
) {}
