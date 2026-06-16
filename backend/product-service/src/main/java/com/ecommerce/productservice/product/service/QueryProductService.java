package com.ecommerce.productservice.product.service;

import com.ecommerce.common.dto.PageResponse;
import com.ecommerce.productservice.product.query.PublicProductInfo;

public interface QueryProductService {
    PublicProductInfo getPublicProductInfo(Long id);

    // Public detay görüntülenmesinde görüntülenme sayacını artırır (best-effort, cache'ten bağımsız).
    void recordView(Long id);

}
