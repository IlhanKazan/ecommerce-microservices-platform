package com.ecommerce.productservice.favorite.service;

import com.ecommerce.productservice.product.query.PublicProductInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface FavoriteService {

    void addFavorite(UUID userId, Long productId);

    void removeFavorite(UUID userId, Long productId);

    Set<Long> getFavoriteProductIds(UUID userId);

    List<PublicProductInfo> getFavoriteProducts(UUID userId);
}
