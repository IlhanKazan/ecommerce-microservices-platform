package com.ecommerce.productservice.category.controller.dto.request;

public record UpdateCategoryRequest(
        String name,
        String description,
        String imageUrl,
        String icon,
        Integer displayOrder,
        Boolean isActive
) {}
