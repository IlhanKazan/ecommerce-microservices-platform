package com.ecommerce.productservice.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TenantStorefrontResponse(
        Long id,
        String name,
        String logoUrl,
        String status
) {}
