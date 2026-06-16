package com.ecommerce.productservice.category.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank(message = "Kategori adı zorunludur")
        String name,
        String description,
        String imageUrl,
        String icon,
        Integer displayOrder,
        Boolean isActive,
        Long parentId
) {}
