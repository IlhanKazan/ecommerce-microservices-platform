package com.ecommerce.productservice.product.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Request to create a product in the tenant catalog")
public record ProductCreateRequest(
        @Schema(description = "Category ID from GET /api/v1/categories", example = "5")
        @NotNull(message = "Kategori ID zorunludur") Long categoryId,
        Long parentProductId,
        @NotBlank(message = "Ürün adı boş olamaz") String name,
        String description,
        @Schema(description = "Unique stock-keeping unit within tenant", example = "TSHIRT-BLU-M")
        @NotBlank(message = "SKU boş olamaz") String sku,
        String brand,
        @Schema(description = "Selling price", example = "299.99")
        @NotNull(message = "Fiyat zorunludur") @Positive BigDecimal price,
        String currency,

        @Schema(description = "Package weight in grams for shipping calculation", example = "500")
        Integer weightGrams,
        String dimensionsCm,

        @Schema(description = "Main product image URL obtained from POST /images/upload", example = "http://localhost:9005/e-commerce-images/products/...")
        String mainImageUrl,
        List<String> imageUrls,
        Map<String, String> attributes,
        Integer minOrderQty,
        Integer maxOrderQty,

        List<String> tags,
        String seoTitle,
        String seoDescription,
        String seoKeywords
) {
    public ProductCreateRequest {
        if (currency == null) currency = "TRY";
        if (minOrderQty == null) minOrderQty = 1;
    }
}