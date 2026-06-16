package com.ecommerce.productservice.product.service;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.productservice.common.service.ImageService;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.controller.dto.response.AdminProductStatsResponse;
import com.ecommerce.productservice.product.controller.dto.response.AdminProductSummaryResponse;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Platform admin ürün yönetimi — istatistik, tüm tenant listesi, moderasyon (deaktif/kaldır).
 * Yetki controller'da (@PreAuthorize hasRole platform-admin). Tenant ownership guard'ı yoktur.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final OutboxService outboxService;
    private final ImageService imageService;

    public AdminProductStatsResponse getStats() {
        long total = productRepository.countByStatusNot(ProductStatus.DELETED);
        long active = productRepository.countByStatus(ProductStatus.ACTIVE);
        return new AdminProductStatsResponse(total, active);
    }

    public Page<AdminProductSummaryResponse> search(String q, Long tenantId, Long categoryId,
                                                    ProductStatus status, Pageable pageable) {
        String like = (q != null && !q.isBlank())
                ? "%" + q.trim().toLowerCase(Locale.of("tr")) + "%"
                : null;
        return productRepository.searchForAdmin(like, tenantId, categoryId, status, pageable)
                .map(this::toSummary);
    }

    /** Admin: ürünü satıştan kaldır (INACTIVE) — search/ES senkronu event ile. */
    @Transactional
    public void deactivateProduct(Long productId) {
        Product product = getActiveProduct(productId);
        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
        outboxService.publishProductUpdatedEvent(product);
        log.info("[ADMIN] Ürün satıştan kaldırıldı (INACTIVE). ProductID: {}", productId);
    }

    /** Admin: ürünü sil (soft delete) — tenant deleteProduct akışıyla aynı (guard'sız). */
    @Transactional
    public void removeProduct(Long productId) {
        Product product = getActiveProduct(productId);

        List<String> images = new ArrayList<>();
        if (product.getMainImageUrl() != null) {
            images.add(product.getMainImageUrl());
        }
        if (product.getImageUrls() != null) {
            images.addAll(product.getImageUrls());
        }

        product.setStatus(ProductStatus.DELETED);
        product.setSalesStatus(SalesStatus.OUT_OF_STOCK);
        productRepository.save(product);

        outboxService.publishProductDeletedEvent(product);
        imageService.deleteImages(images);
        log.info("[ADMIN] Ürün silindi (soft delete). ProductID: {}", productId);
    }

    private Product getActiveProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException("Ürün bulunamadı!", "PRODUCT_NOT_FOUND"));
        if (product.getStatus() == ProductStatus.DELETED) {
            throw new BusinessException("Bu ürün zaten silinmiş!", "PRODUCT_DELETED");
        }
        return product;
    }

    private AdminProductSummaryResponse toSummary(Product p) {
        return new AdminProductSummaryResponse(
                p.getId(),
                p.getTenantId(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getName(),
                p.getSku(),
                p.getPrice(),
                p.getCurrency(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getSalesStatus() != null ? p.getSalesStatus().name() : null,
                p.getMainImageUrl(),
                p.getCreatedAt());
    }
}
