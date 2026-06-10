package com.ecommerce.productservice.category.controller;

import com.ecommerce.productservice.category.controller.dto.response.CategoryResponse;
import com.ecommerce.productservice.category.query.CategoryInfo;
import com.ecommerce.productservice.category.service.CategoryService;
import com.ecommerce.productservice.common.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Categories", description = "Product category hierarchy — no authentication required")
@RestController
@RequestMapping(ApiPaths.Category.CATEGORIES)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List root categories", description = "Returns top-level categories. Each category may include nested subcategories.", security = {})
    @ApiResponse(responseCode = "200", description = "Category list")
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getRootCategories() {
        List<CategoryInfo> categories = categoryService.getRootCategories();
        List<CategoryResponse> response = categories.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get category by slug", description = "Returns a category and its children by URL-friendly slug.", security = {})
    @ApiResponse(responseCode = "200", description = "Category detail")
    @ApiResponse(responseCode = "404", description = "Category not found")
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