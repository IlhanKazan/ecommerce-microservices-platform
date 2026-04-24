package com.ecommerce.productservice.category.service;

import com.ecommerce.productservice.category.query.CategoryInfo;

import java.util.List;

public interface CategoryService {
    List<CategoryInfo> getRootCategories();
    CategoryInfo getCategoryBySlug(String slug);
}
