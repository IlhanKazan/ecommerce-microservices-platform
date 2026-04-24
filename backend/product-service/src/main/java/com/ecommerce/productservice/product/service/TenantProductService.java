package com.ecommerce.productservice.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.productservice.product.constant.SalesStatus;
import com.ecommerce.productservice.product.controller.internal.dto.response.ProductValidationResponse;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.command.ProductCreateContext;
import com.ecommerce.productservice.product.query.ProductInfo;
import com.ecommerce.productservice.product.command.ProductUpdateContext;

public interface TenantProductService {
    Product createProduct(ProductCreateContext context);
    Product updateProduct(Long tenantId, Long productId, ProductUpdateContext context);
    void deleteProduct(Long tenantId, Long productId);
    void changeSalesStatus(Long tenantId, Long productId, SalesStatus newStatus);
    Product getProductByIdAndTenantId(Long productId, Long tenantId);
    ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId);
    PageResponse<ProductInfo> getTenantProducts(Long tenantId, int page, int size);
}
