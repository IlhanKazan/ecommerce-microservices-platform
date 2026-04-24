package com.ecommerce.productservice.category.controller;

import com.ecommerce.productservice.category.controller.dto.response.CategoryResponse;
import com.ecommerce.productservice.category.query.CategoryInfo;
import com.ecommerce.productservice.category.service.CategoryService;
import com.ecommerce.productservice.common.constants.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.Category.CATEGORIES)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        List<CategoryInfo> categories = categoryService.getRootCategories();
        List<CategoryResponse> response = categories.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        CategoryInfo info = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(toResponse(info));
    }

    private CategoryResponse toResponse(CategoryInfo info) {
        List<CategoryResponse> subs = info.subCategories() != null
                ? info.subCategories().stream().map(this::toResponse).toList()
                : List.of();

        return new CategoryResponse(
                info.id(),
                info.name(),
                info.slug(),
                info.description(),
                info.imageUrl(),
                info.icon(),
                info.level(),
                info.fullPath(),
                subs
        );
    }
}