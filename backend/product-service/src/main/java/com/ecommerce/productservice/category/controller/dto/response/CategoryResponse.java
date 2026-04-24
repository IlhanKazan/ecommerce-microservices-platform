package com.ecommerce.productservice.category.controller.dto.response;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String icon,
        Integer level,
        String fullPath,
        List<CategoryResponse> subCategories
) {}