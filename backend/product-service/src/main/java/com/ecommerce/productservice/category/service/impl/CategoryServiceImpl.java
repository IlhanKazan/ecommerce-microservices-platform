package com.ecommerce.productservice.category.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.productservice.category.entity.Category;
import com.ecommerce.productservice.category.query.CategoryInfo;
import com.ecommerce.productservice.category.repository.CategoryRepository;
import com.ecommerce.productservice.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Cacheable(cacheNames = "categories-root")
    public List<CategoryInfo> getRootCategories() {
        log.info("Kök kategoriler DB'den çekiliyor (cache miss)");
        List<Category> roots = categoryRepository
                .findByParentCategoryIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
        return roots.stream().map(this::toInfo).toList();
    }

    @Override
    @Cacheable(cacheNames = "category-by-slug", key = "#slug")
    public CategoryInfo getCategoryBySlug(String slug) {
        log.info("Kategori slug ile çekiliyor: {}", slug);
        Category category = categoryRepository.findBySlugWithSubCategories(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kategori bulunamadı: " + slug, "CATEGORY_NOT_FOUND"));
        return toInfo(category);
    }

    private CategoryInfo toInfo(Category category) {
        List<CategoryInfo> subs = category.getSubCategories().stream()
                .filter(Category::getIsActive)
                .sorted((a, b) -> {
                    if (a.getDisplayOrder() == null) return 1;
                    if (b.getDisplayOrder() == null) return -1;
                    return a.getDisplayOrder().compareTo(b.getDisplayOrder());
                })
                .map(this::toInfo)
                .toList();

        return new CategoryInfo(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getIcon(),
                category.getLevel(),
                category.getFullPath(),
                category.getDisplayOrder(),
                subs
        );
    }
}