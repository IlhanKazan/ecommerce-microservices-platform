package com.ecommerce.productservice.product.service.impl;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.productservice.category.entity.Category;
import com.ecommerce.productservice.category.repository.CategoryRepository;
import com.ecommerce.productservice.common.service.ImageService;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.command.VariantCommand;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.query.ProductDetailInfo;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProductServiceImpl implements TenantProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final OutboxService outboxService;
    private final ImageService imageService;

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
        product.setDiscountedPrice(sanitizeDiscount(context.price(), context.discountedPrice()));

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
        product.setDiscountedPrice(sanitizeDiscount(context.price(), context.discountedPrice()));
        product.setCurrency(context.currency() != null ? context.currency() : "TRY");
        product.setWeightGrams(context.weightGrams());
        product.setDimensionsCm(context.dimensionsCm());

        // Görsel değişiminde MinIO'da orphan kalmasın — eski URL'leri set ETMEDEN önce yakala
        String oldMainImageUrl = product.getMainImageUrl();
        List<String> oldImageUrls = product.getImageUrls() != null
                ? new ArrayList<>(product.getImageUrls())
                : new ArrayList<>();

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

        // Save commit'inden sonra: artık kullanılmayan (yeni listede/main'de olmayan) eski görselleri sil
        deleteOrphanImages(oldMainImageUrl, oldImageUrls, updatedProduct);

        return updatedProduct;
    }

    // ─── Varyant (child product) yönetimi ───────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<Product> getVariants(Long tenantId, Long parentProductId) {
        // Parent'ın tenant'a ait olduğunu doğrula (yetki + 404)
        getProductByIdAndTenantId(parentProductId, tenantId);
        return productRepository.findByParentProductIdAndStatusNot(parentProductId, ProductStatus.DELETED);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #parentProductId"),
            @CacheEvict(cacheNames = "public-product", key = "#parentProductId")
    })
    public Product createVariant(Long tenantId, Long parentProductId, VariantCommand command, java.util.UUID keycloakId) {
        Product parent = getProductByIdAndTenantId(parentProductId, tenantId);

        // Derinlik 1 — bir varyantın altına varyant eklenemez
        if (parent.getParentProduct() != null) {
            throw new BusinessException(
                    "Bir varyantın altına varyant eklenemez.", "INVALID_VARIANT_PARENT");
        }
        if (command.attributes() == null || command.attributes().isEmpty()) {
            throw new BusinessException(
                    "Varyant özellikleri (ör. Renk/Numara) boş olamaz.", "VARIANT_ATTRIBUTES_REQUIRED");
        }

        // Aynı kombinasyonda varyant zaten varsa engelle
        boolean duplicate = productRepository
                .findByParentProductIdAndStatusNot(parentProductId, ProductStatus.DELETED)
                .stream()
                .anyMatch(v -> command.attributes().equals(v.getAttributes()));
        if (duplicate) {
            throw new BusinessException(
                    "Bu özellik kombinasyonunda bir varyant zaten mevcut.", "VARIANT_DUPLICATE");
        }

        Product variant = Product.builder()
                .tenantId(tenantId)
                .category(parent.getCategory())
                .parentProduct(parent)
                .name(resolveVariantName(parent, command))
                .description(parent.getDescription())
                .sku(command.sku())
                .brand(parent.getBrand())
                .price(command.price())
                .currency(parent.getCurrency())
                .mainImageUrl(command.mainImageUrl() != null ? command.mainImageUrl() : parent.getMainImageUrl())
                .attributes(command.attributes())
                .minOrderQty(parent.getMinOrderQty())
                .maxOrderQty(parent.getMaxOrderQty())
                .createdByUserId(keycloakId)
                .build();
        variant.setDiscountedPrice(sanitizeDiscount(command.price(), command.discountedPrice()));

        Product saved = productRepository.save(variant);
        outboxService.publishProductCreatedEvent(saved);
        log.info("Varyant oluşturuldu. ParentID: {}, VariantID: {}", parentProductId, saved.getId());
        return saved;
    }

    // Matris üretici: tek istekte N varyant. Tek transaction → atomik (biri patlarsa hepsi geri alınır).
    // Hem mevcut (DB) hem batch-içi kombinasyon dedup'ı uygulanır.
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #parentProductId"),
            @CacheEvict(cacheNames = "public-product", key = "#parentProductId")
    })
    public List<Product> createVariantsBatch(Long tenantId, Long parentProductId, List<VariantCommand> commands, java.util.UUID keycloakId) {
        if (commands == null || commands.isEmpty()) {
            throw new BusinessException("Varyant listesi boş olamaz.", "VARIANT_BATCH_EMPTY");
        }

        Product parent = getProductByIdAndTenantId(parentProductId, tenantId);
        if (parent.getParentProduct() != null) {
            throw new BusinessException("Bir varyantın altına varyant eklenemez.", "INVALID_VARIANT_PARENT");
        }

        // Mevcut (silinmemiş) kombinasyonlar + batch-içi tekrarları yakalamak için seen set'i
        Set<Map<String, String>> seen = productRepository
                .findByParentProductIdAndStatusNot(parentProductId, ProductStatus.DELETED)
                .stream()
                .map(Product::getAttributes)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        List<Product> toSave = new ArrayList<>();
        for (VariantCommand command : commands) {
            if (command.attributes() == null || command.attributes().isEmpty()) {
                throw new BusinessException(
                        "Varyant özellikleri (ör. Renk/Numara) boş olamaz.", "VARIANT_ATTRIBUTES_REQUIRED");
            }
            if (!seen.add(command.attributes())) {
                throw new BusinessException(
                        "Bu özellik kombinasyonunda bir varyant zaten mevcut: " + command.attributes(),
                        "VARIANT_DUPLICATE");
            }

            Product variant = Product.builder()
                    .tenantId(tenantId)
                    .category(parent.getCategory())
                    .parentProduct(parent)
                    .name(resolveVariantName(parent, command))
                    .description(parent.getDescription())
                    .sku(command.sku())
                    .brand(parent.getBrand())
                    .price(command.price())
                    .currency(parent.getCurrency())
                    .mainImageUrl(command.mainImageUrl() != null ? command.mainImageUrl() : parent.getMainImageUrl())
                    .attributes(command.attributes())
                    .minOrderQty(parent.getMinOrderQty())
                    .maxOrderQty(parent.getMaxOrderQty())
                    .createdByUserId(keycloakId)
                    .build();
            variant.setDiscountedPrice(sanitizeDiscount(command.price(), command.discountedPrice()));
            toSave.add(variant);
        }

        List<Product> saved = productRepository.saveAll(toSave);
        for (Product v : saved) {
            outboxService.publishProductCreatedEvent(v);
        }
        log.info("{} varyant toplu oluşturuldu. ParentID: {}", saved.size(), parentProductId);
        return saved;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", allEntries = true),
            @CacheEvict(cacheNames = "public-product", allEntries = true)
    })
    public Product updateVariant(Long tenantId, Long variantId, VariantCommand command) {
        Product variant = getProductByIdAndTenantId(variantId, tenantId);
        if (variant.getParentProduct() == null) {
            throw new BusinessException("Bu ürün bir varyant değil.", "NOT_A_VARIANT");
        }
        if (command.attributes() == null || command.attributes().isEmpty()) {
            throw new BusinessException(
                    "Varyant özellikleri boş olamaz.", "VARIANT_ATTRIBUTES_REQUIRED");
        }

        // Kombinasyon başka bir varyantta kullanılıyorsa engelle (kendisi hariç)
        boolean duplicate = productRepository
                .findByParentProductIdAndStatusNot(variant.getParentProduct().getId(), ProductStatus.DELETED)
                .stream()
                .anyMatch(v -> !v.getId().equals(variantId) && command.attributes().equals(v.getAttributes()));
        if (duplicate) {
            throw new BusinessException(
                    "Bu özellik kombinasyonunda başka bir varyant mevcut.", "VARIANT_DUPLICATE");
        }

        variant.setSku(command.sku());
        variant.setPrice(command.price());
        variant.setDiscountedPrice(sanitizeDiscount(command.price(), command.discountedPrice()));
        variant.setAttributes(command.attributes());
        if (command.mainImageUrl() != null) {
            variant.setMainImageUrl(command.mainImageUrl());
        }
        variant.setName(resolveVariantName(variant.getParentProduct(), command));

        Product saved = productRepository.save(variant);
        outboxService.publishProductUpdatedEvent(saved);
        log.info("Varyant güncellendi. VariantID: {}", variantId);
        return saved;
    }

    // İsim verilmediyse parent adı + kombinasyon değerlerinden türet (ör. "Klasik Ayakkabı - Siyah / 42")
    private String resolveVariantName(Product parent, VariantCommand command) {
        if (command.name() != null && !command.name().isBlank()) {
            return command.name();
        }
        String combo = String.join(" / ", command.attributes().values());
        return parent.getName() + " - " + combo;
    }

    // İndirimli fiyat ancak pozitif ve asıl fiyattan küçükse geçerli; aksi halde indirim yok (null)
    private java.math.BigDecimal sanitizeDiscount(java.math.BigDecimal price, java.math.BigDecimal discountedPrice) {
        if (price == null || discountedPrice == null) return null;
        if (discountedPrice.compareTo(java.math.BigDecimal.ZERO) <= 0) return null;
        if (discountedPrice.compareTo(price) >= 0) return null;
        return discountedPrice;
    }

    // Eski görsellerden, yeni imageUrls listesinde de yeni mainImageUrl'de de olmayanlar orphan'dır
    private void deleteOrphanImages(String oldMainImageUrl, List<String> oldImageUrls, Product updated) {
        List<String> newImageUrls = updated.getImageUrls() != null ? updated.getImageUrls() : List.of();
        String newMainImageUrl = updated.getMainImageUrl();

        List<String> orphans = new ArrayList<>();
        for (String oldUrl : oldImageUrls) {
            if (oldUrl != null && !newImageUrls.contains(oldUrl) && !oldUrl.equals(newMainImageUrl)) {
                orphans.add(oldUrl);
            }
        }
        if (oldMainImageUrl != null
                && !oldMainImageUrl.equals(newMainImageUrl)
                && !newImageUrls.contains(oldMainImageUrl)
                && !orphans.contains(oldMainImageUrl)) {
            orphans.add(oldMainImageUrl);
        }

        // Paylaşılan görseli koru: parent/kardeş/varyant hâlâ kullanıyorsa MinIO'dan silme.
        Set<String> stillReferenced = relatedImageUrls(updated);
        orphans.removeIf(stillReferenced::contains);

        imageService.deleteImages(orphans);
    }

    // Bu ürünle görsel paylaşabilecek diğer (silinmemiş) ürünlerin görsel URL'leri.
    // Varyant ise parent + kardeş varyantlar; ana ürün ise varyantları. createVariant parent'ın
    // mainImageUrl'ini varyanta kopyaladığından, varyant silme/güncelleme parent görselini uçurmasın.
    private Set<String> relatedImageUrls(Product product) {
        List<Product> related = new ArrayList<>();
        if (product.getParentProduct() != null) {
            Product parent = product.getParentProduct();
            related.add(parent);
            related.addAll(productRepository.findByParentProductIdAndStatusNot(parent.getId(), ProductStatus.DELETED));
        } else {
            related.addAll(productRepository.findByParentProductIdAndStatusNot(product.getId(), ProductStatus.DELETED));
        }

        Set<String> urls = new HashSet<>();
        for (Product p : related) {
            if (p.getId().equals(product.getId())) {
                continue; // kendisi hariç
            }
            if (p.getMainImageUrl() != null) {
                urls.add(p.getMainImageUrl());
            }
            if (p.getImageUrls() != null) {
                urls.addAll(p.getImageUrls());
            }
        }
        return urls;
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

        // Soft delete öncesi görsel URL'lerini topla (DELETED terminal; restore akışı yok → silmek güvenli)
        List<String> images = new ArrayList<>();
        if (product.getMainImageUrl() != null) {
            images.add(product.getMainImageUrl());
        }
        if (product.getImageUrls() != null) {
            images.addAll(product.getImageUrls());
        }

        // Paylaşılan görseli koru: parent/kardeş/varyant hâlâ kullanıyorsa MinIO'dan silme
        // (ör. varyant parent'ın mainImageUrl'ini paylaşıyorsa, varyant silinince parent görseli uçmasın).
        Set<String> stillReferenced = relatedImageUrls(product);
        images.removeIf(stillReferenced::contains);

        product.setStatus(ProductStatus.DELETED);
        product.setSalesStatus(SalesStatus.OUT_OF_STOCK);

        productRepository.save(product);

        outboxService.publishProductDeletedEvent(product);

        // Save sonrası: ürünün tüm görsellerini MinIO'dan temizle
        imageService.deleteImages(images);
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

        List<Long> variantIds = productRepository
                .findVariantIdsByParentIdAndStatus(product.getId(), ProductStatus.ACTIVE);

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
                product.getAttributes(),
                !variantIds.isEmpty(),
                variantIds,
                product.getViewCount(),
                product.getSaleCount(),
                product.getIsFeatured()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, Long tenantId) {
        Product product = getProductByIdAndTenantId(productId, tenantId);
        return new ProductDetailInfo(
                product.getId(),
                product.getTenantId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getParentProduct() != null ? product.getParentProduct().getId() : null,
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getBrand(),
                product.getPrice(),
                product.getDiscountedPrice(),
                product.getCurrency(),
                product.getMainImageUrl(),
                product.getImageUrls(),
                product.getAttributes(),
                product.getWeightGrams(),
                product.getDimensionsCm(),
                product.getMinOrderQty(),
                product.getMaxOrderQty(),
                product.getTags(),
                product.getSeoTitle(),
                product.getSeoDescription(),
                product.getSeoKeywords(),
                product.getStatus(),
                product.getSalesStatus(),
                product.getIsFeatured(),
                product.getRatingAverage(),
                product.getReviewCount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductInfo> getTenantProducts(Long tenantId, String q, String salesStatus, String sort, int page, int size) {
        // Sıralama Pageable üzerinden (JPQL'de ORDER BY yok). Varsayılan: en yeni.
        Sort sortSpec = switch (sort == null ? "newest" : sort) {
            case "sales" -> Sort.by(Sort.Direction.DESC, "saleCount");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        // q: LIKE deseni (lowercase, %...%) ya da null. salesStatus: enum ya da null.
        String likePattern = (q == null || q.isBlank())
                ? null
                : "%" + q.trim().toLowerCase() + "%";
        SalesStatus statusFilter = (salesStatus == null || salesStatus.isBlank())
                ? null
                : SalesStatus.valueOf(salesStatus);

        Page<Product> products = productRepository
                .searchForTenant(tenantId, likePattern, statusFilter, pageable);

        // Sayfadaki ana ürünlerin ACTIVE varyant id'lerini tek sorguda topla (N+1 yok).
        List<Long> parentIds = products.getContent().stream().map(Product::getId).toList();
        Map<Long, List<Long>> variantIdsByParent = parentIds.isEmpty()
                ? Map.of()
                : productRepository.findVariantIdRowsByParentIds(parentIds, ProductStatus.ACTIVE).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        row -> (Long) row[0],
                        java.util.stream.Collectors.mapping(row -> (Long) row[1], java.util.stream.Collectors.toList())));

        Page<ProductInfo> infoPage = products.map(product -> {
            List<Long> variantIds = variantIdsByParent.getOrDefault(product.getId(), List.of());
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
                    product.getAttributes(),
                    !variantIds.isEmpty(),
                    variantIds,
                    product.getViewCount(),
                    product.getSaleCount(),
                    product.getIsFeatured()
            );
        });

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

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId"),
            @CacheEvict(cacheNames = "public-product", key = "#productId")
    })
    public void setFeatured(Long tenantId, Long productId, boolean featured) {
        Product product = getProductByIdAndTenantId(productId, tenantId);

        // Varyant (child) öne çıkarılamaz — vitrin yalnızca ana ürünleri gösterir.
        if (product.getParentProduct() != null) {
            throw new BusinessException(
                    "Varyant öne çıkarılamaz; ana ürünü öne çıkarın.", "CANNOT_FEATURE_VARIANT");
        }

        product.setIsFeatured(featured);
        productRepository.save(product);

        outboxService.publishProductUpdatedEvent(product);
        log.info("Öne çıkan durumu güncellendi. Tenant: {}, Product: {}, featured: {}",
                tenantId, productId, featured);
    }

}
