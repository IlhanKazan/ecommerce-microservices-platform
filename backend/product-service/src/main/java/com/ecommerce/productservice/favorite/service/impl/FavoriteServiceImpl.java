package com.ecommerce.productservice.favorite.service.impl;

import com.ecommerce.productservice.favorite.entity.ProductFavorite;
import com.ecommerce.productservice.favorite.repository.ProductFavoriteRepository;
import com.ecommerce.productservice.favorite.service.FavoriteService;
import com.ecommerce.productservice.product.query.PublicProductInfo;
import com.ecommerce.productservice.product.service.QueryProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {

    private final ProductFavoriteRepository favoriteRepository;
    private final QueryProductService queryProductService;

    @Override
    @Transactional
    public void addFavorite(UUID userId, Long productId) {
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            return; // idempotent
        }
        favoriteRepository.save(ProductFavorite.builder()
                .userId(userId)
                .productId(productId)
                .build());
        log.info("Favori eklendi — userId: {}, productId: {}", userId, productId);
    }

    @Override
    @Transactional
    public void removeFavorite(UUID userId, Long productId) {
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Favori çıkarıldı — userId: {}, productId: {}", userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getFavoriteProductIds(UUID userId) {
        return favoriteRepository.findProductIdsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProductInfo> getFavoriteProducts(UUID userId) {
        List<PublicProductInfo> result = new ArrayList<>();
        for (ProductFavorite favorite : favoriteRepository.findAllByUserIdOrderByCreatedAtDesc(userId)) {
            try {
                // getPublicProductInfo ACTIVE olmayan/silinen ürünü reddeder — bunları listeden düşürürüz
                result.add(queryProductService.getPublicProductInfo(favorite.getProductId()));
            } catch (Exception e) {
                log.debug("Favori ürün listelenemiyor (silinmiş/pasif), atlanıyor — productId: {}", favorite.getProductId());
            }
        }
        return result;
    }
}
