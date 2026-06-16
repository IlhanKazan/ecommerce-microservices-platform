package com.ecommerce.productservice.product.controller.dto.response;

/**
 * Platform admin overview ürün sayımları.
 * totalProducts = DELETED hariç tüm ürünler; activeProducts = ACTIVE durumdakiler.
 */
public record AdminProductStatsResponse(
        long totalProducts,
        long activeProducts
) {}
