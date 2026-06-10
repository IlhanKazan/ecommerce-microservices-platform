package com.ecommerce.productservice.product.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.query.ProductValidationInfo;
import com.ecommerce.productservice.product.repository.ProductRepository;
import com.ecommerce.productservice.product.service.InternalProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalProductServiceImpl implements InternalProductService {

    private final ProductRepository productRepository;
    private final OutboxService outboxService;

    @Override
    @Transactional(readOnly = true)
    public ProductValidationInfo validateAndGetProduct(Long productId, Long tenantId) {
        log.info("[INTERNAL] Ürün doğrulanıyor. ProductId: {}, TenantId: {}",
                productId, tenantId);

        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ürün bulunamadı veya bu mağazaya ait değil.",
                        "PRODUCT_NOT_FOUND"));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new BusinessException(
                    "Bu ürün silinmiş.", "PRODUCT_DELETED");
        }

        if (product.getSalesStatus() == SalesStatus.OUT_OF_STOCK) {
            throw new BusinessException(
                    "Bu ürün stokta yok.", "PRODUCT_OUT_OF_STOCK");
        }

        return new ProductValidationInfo(
                product.getId(),
                product.getTenantId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                product.getCurrency(),
                product.getStatus().name(),
                product.getSalesStatus().name(),
                product.getMainImageUrl()
        );
    }

    @Override
    @Transactional
    public int reindexAllProducts() {
        List<Product> products = productRepository.findAllByStatus(ProductStatus.ACTIVE);
        log.info("[REINDEX] {} adet aktif ürün için PRODUCT_UPDATED event yayınlanıyor...", products.size());
        for (Product product : products) {
            try {
                outboxService.publishProductUpdatedEvent(product);
            } catch (Exception e) {
                log.error("[REINDEX] Ürün ID {} için event yayınlanamadı: {}", product.getId(), e.getMessage());
            }
        }
        log.info("[REINDEX] Tamamlandı. {} ürün için event yazıldı.", products.size());
        return products.size();
    }

    @Override
    @Transactional
    public void updateAiReport(Long productId, String aiReviewReport) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ürün bulunamadı.", "PRODUCT_NOT_FOUND"));
        product.setAiReviewReport(aiReviewReport);
        productRepository.save(product);
        log.info("[AI-REPORT] AI raporu güncellendi. ProductId: {}", productId);
    }
}