package com.ecommerce.productservice.product.service.impl;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.productservice.category.entity.Category;
import com.ecommerce.productservice.category.repository.CategoryRepository;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.query.ProductInfo;
import com.ecommerce.productservice.product.command.ProductUpdateContext;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.repository.ProductRepository;
import com.ecommerce.productservice.product.service.TenantProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProductServiceImpl implements TenantProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public Product createProduct(ProductCreateContext context) {
        log.info("Tenant [{}] için yeni ürün ekleniyor. İşlemi Yapan Kullanıcı ID: {}, SKU: {}",
                context.tenantId(), context.keycloakId(), context.sku());

        Category category = categoryRepository.findById(context.categoryId())
                .orElseThrow(() -> {
                    log.error("HATA: Kategori bulunamadı. ID: {}", context.categoryId());
                    return new RuntimeException("Belirtilen kategori bulunamadı!");
                });

        Product product = productMapper.toEntity(context);
        product.setCategory(category);
        product.setCreatedByUserId(context.keycloakId());

        if (context.parentProductId() != null) {
            Product parent = productRepository.findByIdAndTenantId(context.parentProductId(), context.tenantId())
                    .orElseThrow(() -> {
                        log.error("HATA: Ana ürün bulunamadı veya Tenant [{}] yetkisi yok. Parent ID: {}",
                                context.tenantId(), context.parentProductId());
                        return new RuntimeException("Ana ürün bulunamadı veya bu mağazaya ait değil!");
                    });
            product.setParentProduct(parent);
            log.info("Ürün, [{}] ID'li ana ürünün varyantı olarak ayarlandı.", parent.getId());
        }

        Product savedProduct = productRepository.save(product);
        log.info("Ürün başarıyla kaydedildi. Veritabanı ID: {}", savedProduct.getId());

        outboxService.publishProductCreatedEvent(savedProduct);

        return savedProduct;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId"),
            @CacheEvict(cacheNames = "public-product", key = "#productId")
    })
    public Product updateProduct(Long tenantId, Long productId, ProductUpdateContext context) {
        log.info("Ürün güncelleniyor. Tenant: {}, Product: {}", tenantId, productId);

        Product product = getProductByIdAndTenantId(productId, tenantId);

        // Kategori değiştiyse yükle
        if (context.categoryId() != null
                && !product.getCategory().getId().equals(context.categoryId())) {
            Category newCategory = categoryRepository.findById(context.categoryId())
                    .orElseThrow(() -> new BusinessException(
                            "Kategori bulunamadı.", "CATEGORY_NOT_FOUND"));
            product.setCategory(newCategory);
        }

        // Parent değiştiyse doğrula — sadece aynı tenant'a ait olabilir
        if (context.parentProductId() != null
                && !context.parentProductId().equals(
                product.getParentProduct() != null
                        ? product.getParentProduct().getId() : null)) {
            Product parent = productRepository
                    .findByIdAndTenantId(context.parentProductId(), tenantId)
                    .orElseThrow(() -> new BusinessException(
                            "Ana ürün bulunamadı veya bu mağazaya ait değil.",
                            "PARENT_PRODUCT_NOT_FOUND"));
            product.setParentProduct(parent);
        }

        product.setName(context.name());
        product.setDescription(context.description());
        product.setSku(context.sku());
        product.setBrand(context.brand());
        product.setPrice(context.price());
        product.setCurrency(context.currency() != null ? context.currency() : "TRY");
        product.setWeightGrams(context.weightGrams());
        product.setDimensionsCm(context.dimensionsCm());
        product.setMainImageUrl(context.mainImageUrl());
        product.setImageUrls(context.imageUrls());
        product.setAttributes(context.attributes());
        product.setMinOrderQty(context.minOrderQty());
        product.setMaxOrderQty(context.maxOrderQty());
        product.setTags(context.tags());
        product.setSeoTitle(context.seoTitle());
        product.setSeoDescription(context.seoDescription());
        product.setSeoKeywords(context.seoKeywords());

        // Status/SalesStatus — sadece geçerli geçişlere izin ver
        if (context.status() != null) {
            ProductStatus newStatus = ProductStatus.valueOf(context.status());
            if (newStatus == ProductStatus.DELETED) {
                throw new BusinessException(
                        "Ürün silme işlemi için DELETE endpoint'ini kullanın.",
                        "INVALID_STATUS_TRANSITION");
            }
            product.setStatus(newStatus);
        }

        Product updatedProduct = productRepository.save(product);
        outboxService.publishProductUpdatedEvent(updatedProduct);

        return updatedProduct;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId"),
            @CacheEvict(cacheNames = "public-product", key = "#productId")
    })
    public void deleteProduct(Long tenantId, Long productId) {
        log.info("Ürün siliniyor (Soft Delete). Tenant: {}, Product: {}", tenantId, productId);

        Product product = this.getProductByIdAndTenantId(productId, tenantId);

        if (product.getStatus() == ProductStatus.DELETED) {
            return;
        }

        product.setStatus(ProductStatus.DELETED);
        product.setSalesStatus(SalesStatus.OUT_OF_STOCK);

        productRepository.save(product);

        outboxService.publishProductDeletedEvent(product);
    }

    @Override
    public Product getProductByIdAndTenantId(Long productId, Long tenantId){
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new BusinessException("Ürün bulunamadı veya bu dükkana ait değil!", "PRODUCT_NOT_FOUND"));

        if (product.getStatus() == ProductStatus.DELETED) {
            throw new BusinessException("Bu ürün silinmiş!", "PRODUCT_DELETED");
        }

        return product;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId")
    public ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId) {

        Product product = this.getProductByIdAndTenantId(productId, tenantId);

        return new ProductInfo(
                product.getId(),
                product.getTenantId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getParentProduct() != null
                        ? product.getParentProduct().getId() : null,
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getCurrency(),
                product.getMainImageUrl(),
                product.getStatus(),
                product.getSalesStatus(),
                product.getAttributes()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductInfo> getTenantProducts(Long tenantId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Product> products = productRepository
                .findAllByTenantIdAndStatusNot(tenantId, ProductStatus.DELETED, pageable);

        Page<ProductInfo> infoPage = products.map(product -> new ProductInfo(
                product.getId(),
                product.getTenantId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getParentProduct() != null
                        ? product.getParentProduct().getId() : null,
                product.getName(),
                product.getSku(),
                product.getPrice(),
                product.getCurrency(),
                product.getMainImageUrl(),
                product.getStatus(),
                product.getSalesStatus(),
                product.getAttributes()
        ));

        return PageResponse.of(infoPage);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId"),
            @CacheEvict(cacheNames = "public-product", key = "#productId")
    })
    public void changeSalesStatus(Long tenantId, Long productId, SalesStatus newStatus) {
        log.info("Satış durumu değiştiriliyor. Tenant: {}, Product: {}, Yeni Durum: {}",
                tenantId, productId, newStatus);

        Product product = getProductByIdAndTenantId(productId, tenantId);

        // Silinmiş ürünün satış durumu değiştirilemez
        if (product.getStatus() == ProductStatus.DELETED) {
            throw new BusinessException(
                    "Silinmiş ürünün satış durumu değiştirilemez.", "PRODUCT_DELETED");
        }

        product.setSalesStatus(newStatus);
        productRepository.save(product);

        outboxService.publishProductUpdatedEvent(product);
    }

}
