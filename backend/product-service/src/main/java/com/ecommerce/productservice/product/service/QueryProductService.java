package com.ecommerce.productservice.product.service;

import com.ecommerce.productservice.product.entity.PublicProductInfo;

public interface QueryProductService {
    PublicProductInfo getPublicProductInfo(Long id);
}
