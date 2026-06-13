package com.ecommerce.searchservice.product.query;

/** Marka facet — bir markanın mevcut filtre bağlamındaki ürün sayısı. */
public record BrandFacet(String brand, long count) {}
