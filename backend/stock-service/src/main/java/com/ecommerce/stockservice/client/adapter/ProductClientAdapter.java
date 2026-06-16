package com.ecommerce.stockservice.client.adapter;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.stockservice.client.ProductClient;
import com.ecommerce.stockservice.client.dto.ProductResponse;
import com.ecommerce.stockservice.client.dto.StockGroupResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /**
     * ES stok agregasyon grubunu çözer. Best-effort: product-service'e ulaşılamazsa
     * ürünün kendisini hedef alan grupla geri döner (ES senkronu o tur atlanır ama
     * çekirdek stok işlemi kesintiye uğramaz).
     */
    public StockGroupResponse resolveStockGroup(Long productId) {
        try {
            StockGroupResponse group = productClient.resolveStockGroup(productId);
            if (group == null || group.searchTargetId() == null
                    || group.memberIds() == null || group.memberIds().isEmpty()) {
                return new StockGroupResponse(productId, List.of(productId));
            }
            return group;
        } catch (Exception e) {
            log.warn("Stok grubu çözülemedi, ürünün kendisi hedef alınıyor. ProductID: {}, Hata: {}",
                    productId, e.getMessage());
            return new StockGroupResponse(productId, List.of(productId));
        }
    }
}