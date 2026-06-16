package com.ecommerce.productservice.category.controller;

import com.ecommerce.productservice.category.command.CategoryCommand;
import com.ecommerce.productservice.category.controller.dto.request.CreateCategoryRequest;
import com.ecommerce.productservice.category.controller.dto.request.UpdateCategoryRequest;
import com.ecommerce.productservice.category.controller.dto.response.AdminCategoryResponse;
import com.ecommerce.productservice.category.query.CategoryInfo;
import com.ecommerce.productservice.category.service.CategoryService;
import com.ecommerce.productservice.common.constants.ApiPaths;
import com.ecommerce.productservice.common.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Admin — Categories", description = "Platform admin category management — CRUD on the category tree (requires platform-admin role)")
@RestController
@RequestMapping(ApiPaths.Category.ADMIN_CATEGORIES)
@RequiredArgsConstructor
@PreAuthorize("hasRole('platform-admin')")
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final ImageService imageService;

    @Operation(summary = "List all categories (admin)", description = "Full category tree including inactive categories.")
    @ApiResponse(responseCode = "200", description = "Category tree")
    @GetMapping
    public ResponseEntity<List<AdminCategoryResponse>> list() {
        List<AdminCategoryResponse> response = categoryService.getAllForAdmin().stream()
                .map(this::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Create category", description = "Creates a category. Slug is auto-generated from the name; level/fullPath derived from the optional parent.")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "404", description = "Parent category not found")
    @PostMapping
    public ResponseEntity<AdminCategoryResponse> create(@RequestBody @Valid CreateCategoryRequest request) {
        CategoryCommand command = new CategoryCommand(
                request.name(), request.description(), request.imageUrl(), request.icon(),
                request.displayOrder(), request.isActive(), request.parentId());
        AdminCategoryResponse response = toResponse(categoryService.create(command));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Update category", description = "Updates display fields. Slug and parent are immutable.")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PutMapping("/{id}")
    public ResponseEntity<AdminCategoryResponse> update(
            @PathVariable Long id,
            @RequestBody UpdateCategoryRequest request) {
        CategoryCommand command = new CategoryCommand(
                request.name(), request.description(), request.imageUrl(), request.icon(),
                request.displayOrder(), request.isActive(), null);
        return ResponseEntity.ok(toResponse(categoryService.update(id, command)));
    }

    @Operation(summary = "Set category active status")
    @ApiResponse(responseCode = "200", description = "Status updated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminCategoryResponse> setStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(toResponse(categoryService.setStatus(id, active)));
    }

    @Operation(summary = "Upload category image", description = "Uploads a category image to MinIO and sets imageUrl. Max 5MB, JPEG/PNG/WebP.")
    @ApiResponse(responseCode = "200", description = "Image uploaded")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdminCategoryResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        String imageUrl = imageService.uploadImage(file, "categories");
        return ResponseEntity.ok(toResponse(categoryService.setImage(id, imageUrl)));
    }

    @Operation(summary = "Delete category", description = "Deletes a category. Returns 409 if it has subcategories or linked products.")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @ApiResponse(responseCode = "409", description = "Category has subcategories or products")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminCategoryResponse toResponse(CategoryInfo info) {
        List<AdminCategoryResponse> subs = info.subCategories() != null
                ? info.subCategories().stream().map(this::toResponse).toList()
                : List.of();
        return new AdminCategoryResponse(
                info.id(),
                info.name(),
                info.slug(),
                info.description(),
                info.imageUrl(),
                info.icon(),
                info.level(),
                info.fullPath(),
                info.displayOrder(),
                info.isActive(),
                info.parentId(),
                subs
        );
    }
}
