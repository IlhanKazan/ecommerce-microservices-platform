package com.ecommerce.productservice.product.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.productservice.product.constant.ProductStatus;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.query.PublicProductInfo;
import com.ecommerce.productservice.product.repository.ProductRepository;
import com.ecommerce.productservice.product.service.QueryProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QueryProductServiceImpl implements QueryProductService {

    private final ProductRepository productRepository;

    @Override
    @Cacheable(cacheNames = "public-product", key = "#id")
    public PublicProductInfo getPublicProductInfo(Long id) {
        log.info("Ürün DB'den sorgulanıyor (Cache Miss!). ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ürün bulunamadı.", "PRODUCT_NOT_FOUND"));

        if (product.getStatus() != ProductStatus.ACTIVE || product.getSalesStatus() != SalesStatus.ON_SALE) {
            throw new BusinessException("Bu ürün şu an satışta değil.", "PRODUCT_NOT_AVAILABLE");
        }

        return new PublicProductInfo(
                product.getId(),
                product.getTenantId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getParentProduct() != null
                        ? product.getParentProduct().getId() : null,
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
                product.getRatingAverage(),
                product.getReviewCount(),
                product.getMinOrderQty(),
                product.getMaxOrderQty(),
                product.getStatus().toString(),
                product.getSalesStatus().toString()
        );
    }

}