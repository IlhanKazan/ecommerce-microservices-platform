package com.ecommerce.productservice.product.service;

import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.entity.ProductCreateContext;
import com.ecommerce.productservice.product.entity.ProductInfo;
import com.ecommerce.productservice.product.entity.ProductUpdateContext;

public interface TenantProductService {
    Product createProduct(ProductCreateContext context);
    Product updateProduct(Long tenantId, Long productId, ProductUpdateContext context);
    void deleteProduct(Long tenantId, Long productId);
    Product getProductByIdAndTenantId(Long productId, Long tenantId);
    ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId);
}
