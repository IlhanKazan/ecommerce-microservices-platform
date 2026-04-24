package com.ecommerce.productservice.category.query;

import java.io.Serializable;
import java.util.List;

public record CategoryInfo(
        Long id,
        String name,
        String slug,
        String description,
        String imageUrl,
        String icon,
        Integer level,
        String fullPath,
        Integer displayOrder,
        List<CategoryInfo> subCategories
) implements Serializable {}