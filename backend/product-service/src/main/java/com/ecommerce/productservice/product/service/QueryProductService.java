package com.ecommerce.productservice.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.productservice.product.query.PublicProductInfo;

public interface QueryProductService {
    PublicProductInfo getPublicProductInfo(Long id);

}
