package com.ecommerce.usertenantservice.activity.service;

import java.util.List;
import java.util.UUID;

public interface UserActivityService {
    // Giriş yapmış kullanıcının ürün görüntülemesini kaydeder (öneri/AI verisi).
    void recordProductView(UUID userId, Long productId, Long tenantId);

    // Son gezilen ürün id'leri (en yeni önce, distinct).
    List<Long> getRecentlyViewedProductIds(UUID userId, int limit);

    // Arama terimi kaydı + son aramalar.
    void recordSearch(UUID userId, String term);
    List<String> getRecentSearches(UUID userId, int limit);
}
