package com.ecommerce.searchservice.product.service;

import com.ecommerce.searchservice.product.controller.dto.ProductSearchRequest;
import com.ecommerce.searchservice.product.document.ProductDocument;
import com.ecommerce.searchservice.product.query.AutocompleteSuggestionInfo;
import com.ecommerce.searchservice.product.query.BrandFacet;
import com.ecommerce.searchservice.product.query.ProductAvailabilityInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductSearchService {
    Page<ProductDocument> searchProducts(ProductSearchRequest request);
    // Id listesiyle ürün hidrasyonu — giriş sırasını korur (rail'ler için).
    List<ProductDocument> findByIds(List<Long> ids);
    // Benzer ürünler (içerik-tabanlı): aynı kategori + marka boost, popülerliğe göre, kendini hariç tutar.
    List<ProductDocument> findSimilar(Long productId, int size);
    // İlgili ürünler (seed listesine göre): "senin için" fallback + tohum-bazlı öneri. excludeIds hariç tutulur.
    List<ProductDocument> findRelated(List<Long> seedIds, List<Long> excludeIds, int size);
    List<AutocompleteSuggestionInfo> autocomplete(String q, int size);
    List<BrandFacet> getBrandFacets(ProductSearchRequest request);
    ProductAvailabilityInfo getAvailability(String id);
}
