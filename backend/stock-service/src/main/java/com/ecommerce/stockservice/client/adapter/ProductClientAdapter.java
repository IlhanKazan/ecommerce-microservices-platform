package com.ecommerce.stockservice.client.adapter;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.stockservice.client.ProductClient;
import com.ecommerce.stockservice.client.dto.ProductResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClientAdapter {

    private final ProductClient productClient;

    public ProductResponse validateAndGetProduct(Long productId, Long tenantId) {
        try {
            return productClient.getTenantProduct(tenantId, productId);
        } catch (FeignException.NotFound | FeignException.BadRequest e) {
            log.warn("Güvenlik/Bulunamama İhlali! Tenant {} ürüne ({}) stok eklemeye çalıştı!", tenantId, productId);
            throw new BusinessException("Bu ürün size ait değil veya bulunamadı!", "PRODUCT_NOT_OWNED");
        } catch (Exception e) {
            log.error("Product Service'e ulaşılamadı. ProductID: {}", productId, e);
            throw new ExternalServiceException("Ürün doğrulaması yapılamadı, lütfen sonra tekrar deneyin.", "PRODUCT_SERVICE_DOWN");
        }
    }
}