package com.ecommerce.productservice.category.controller.dto.response;

import java.util.List;

public record AdminCategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String icon,
        Integer level,
        String fullPath,
        Integer displayOrder,
        Boolean isActive,
        Long parentId,
        List<AdminCategoryResponse> subCategories
) {}
