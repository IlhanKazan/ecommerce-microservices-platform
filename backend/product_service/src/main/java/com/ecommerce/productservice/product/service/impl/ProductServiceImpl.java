package com.ecommerce.productservice.product.service.impl;

import com.ecommerce.productservice.category.entity.Category;
import com.ecommerce.productservice.category.repository.CategoryRepository;
import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.entity.ProductCreateContext;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.mapper.ProductMapper;
import com.ecommerce.productservice.product.repository.ProductRepository;
import com.ecommerce.productservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

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
}
