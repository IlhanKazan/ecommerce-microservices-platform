package com.ecommerce.productservice.product.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.productservice.client.adapter.UserTenantClientAdapter;
import com.ecommerce.productservice.client.dto.TenantStorefrontResponse;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.query.ProductValidationInfo;
import com.ecommerce.productservice.product.query.StockGroupInfo;
import com.ecommerce.productservice.product.repository.ProductRepository;
import com.ecommerce.productservice.product.service.InternalProductService;
import com.ecommerce.productservice.product.util.PriceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalProductServiceImpl implements InternalProductService {

    private final ProductRepository productRepository;
    private final OutboxService outboxService;
    private final UserTenantClientAdapter userTenantClientAdapter;

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

        // Varyantı olan ana ürün doğrudan satılamaz — müşteri bir varyant (ör. beden/numara) seçmeli.
        // Stok ve satış varyant (child) üzerinden yürür; numarasız parent siparişi engellenir.
        boolean hasVariants = productRepository.existsByParentProductIdAndStatus(
                product.getId(), ProductStatus.ACTIVE);
        if (hasVariants) {
            throw new BusinessException(
                    "Bu ürün için lütfen bir seçenek (ör. beden/numara) seçin.",
                    "VARIANT_SELECTION_REQUIRED");
        }

        // Mağaza duraklatılmış/kapalıysa satın alma engellenir. UTS'ye ulaşılamazsa (storefront null)
        // fail-open: checkout'u kilitlemektense canlı durumu bilmediğimizde geçir.
        TenantStorefrontResponse storefront = userTenantClientAdapter.getStorefront(tenantId);
        if (storefront != null && storefront.status() != null
                && !"ACTIVE".equals(storefront.status())) {
            throw new BusinessException(
                    "Bu mağaza şu anda satış yapmıyor.", "STORE_NOT_AVAILABLE");
        }

        // Checkout indirimli fiyattan tahsil edilmeli — geçerli indirim varsa onu, yoksa liste fiyatını döndür.
        return new ProductValidationInfo(
                product.getId(),
                product.getTenantId(),
                product.getSku(),
                product.getName(),
                PriceCalculator.effectivePrice(product.getPrice(), product.getDiscountedPrice()),
                product.getCurrency(),
                product.getStatus().name(),
                product.getSalesStatus().name(),
                product.getMainImageUrl(),
                hasVariants
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
    @Transactional(readOnly = true)
    public StockGroupInfo resolveStockGroup(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            // Defensif: ürün yoksa kendi id'siyle dön (search bu id'yi bulamazsa no-op).
            return new StockGroupInfo(productId, List.of(productId));
        }

        // Varyant (child) ise hedef parent'tır; inStock'u parent'ın TÜM aktif varyantları belirler.
        if (product.getParentProduct() != null) {
            Long parentId = product.getParentProduct().getId();
            List<Long> members = productRepository.findVariantIdsByParentIdAndStatus(parentId, ProductStatus.ACTIVE);
            return new StockGroupInfo(parentId, members.isEmpty() ? List.of(productId) : members);
        }

        // Parent/standalone: varyantı varsa inStock'u aktif varyantlar belirler, yoksa ürünün kendi stoğu.
        List<Long> activeVariantIds = productRepository.findVariantIdsByParentIdAndStatus(productId, ProductStatus.ACTIVE);
        return new StockGroupInfo(productId, activeVariantIds.isEmpty() ? List.of(productId) : activeVariantIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getSalesAggregationIds(Long productId) {
        // Ürünün kendisi (standalone/parent) + tüm varyant child'ları (DELETED dahil — geçmiş satış)
        List<Long> ids = new ArrayList<>();
        ids.add(productId);
        ids.addAll(productRepository.findVariantIdsByParentId(productId));
        return ids;
    }

    @Override
    @Transactional
    public void recordSales(java.util.List<com.ecommerce.contracts.event.order.OrderItemSnapshotPayload> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (var item : items) {
            if (item.productId() == null || item.quantity() == null || item.quantity() <= 0) {
                continue;
            }
            // Varyant satıldıysa sale_count parent'ta toplanır (katalog/popülerlik parent bazında).
            Long catalogId = productRepository.findCatalogProductId(item.productId()).orElse(item.productId());
            productRepository.incrementSaleCount(catalogId, item.quantity());
        }
        log.info("[SALES] {} kalem için sale_count güncellendi.", items.size());
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