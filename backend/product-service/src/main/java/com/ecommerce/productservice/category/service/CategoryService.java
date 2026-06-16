package com.ecommerce.productservice.category.service;

import com.ecommerce.productservice.category.command.CategoryCommand;
import com.ecommerce.productservice.category.query.CategoryInfo;

import java.util.List;

public interface CategoryService {
    List<CategoryInfo> getRootCategories();
    CategoryInfo getCategoryBySlug(String slug);

    // --- Platform admin (CRUD) ---
    List<CategoryInfo> getAllForAdmin();
    CategoryInfo create(CategoryCommand command);
    CategoryInfo update(Long id, CategoryCommand command);
    CategoryInfo setStatus(Long id, boolean active);
    CategoryInfo setImage(Long id, String imageUrl);
    void delete(Long id);
}
