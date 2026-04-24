package com.ecommerce.basketservice.client.adapter;

import com.ecommerce.basketservice.client.ProductClient;
import com.ecommerce.basketservice.client.dto.ProductClientResponse;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClientAdapter {

    private final ProductClient productClient;

    // Yetersiz stok diyebilmek için sepetten buraya miktar da yolluyoruz
    public ProductClientResponse validateAndGetProduct(Long productId, Integer requestedQuantity) {
        log.info("Product Service'ten ürün bilgisi doğrulanıyor. ProductId: {}", productId);

        try {
            ProductClientResponse response = productClient.getProductById(productId);

            if (response == null) {
                log.warn("Product Service boş döndü. ProductId: {}", productId);
                throw new ResourceNotFoundException("Ürün veritabanında bulunamadı. ID: " + productId, "404");
            }

            // Aktif mi? Satışta mı?
            if (!"ACTIVE".equals(response.status()) || "OUT_OF_STOCK".equals(response.salesStatus())) {
                log.warn("Ürün satışa kapalı. ProductId: {}, Status: {}, SalesStatus: {}, Rastgele: {}",
                        productId, response.status(), response.salesStatus(), response.name());

                throw new BusinessException(
                        "Bu ürün şu an satışa kapalı veya tükendi.",
                        "PRODUCT_NOT_AVAILABLE",
                        Map.of("productId", productId, "salesStatus", response.salesStatus())
                );
            }

            // Eğer ProductResponse içine stockQuantity eklersem bu blogu yorumdan cekicem
            /* if (response.stockQuantity() != null && response.stockQuantity() < requestedQuantity) {
                throw new BusinessException(
                        "Yetersiz stok! Sadece " + response.stockQuantity() + " adet ekleyebilirsiniz.",
                        "INSUFFICIENT_STOCK",
                        Map.of("requested", requestedQuantity, "available", response.stockQuantity())
                );
            }
            */

            log.info("Ürün doğrulandı. Fiyat: {}", response.price());
            return response;

        } catch (FeignException.NotFound e) {
            log.warn("Ürün Product Service'te bulunamadı (HTTP 404). ProductId: {}", productId);
            throw new ResourceNotFoundException("Sepete eklenmek istenen ürün mevcut değil.", "404");

        } catch (FeignException e) {
            log.error("Product Service ile iletişim kurulamadı. Status: {}", e.status());
            throw new ExternalServiceException("Ürün bilgileri doğrulanırken servise ulaşılamadı.", "PRODUCT_SERVICE_DOWN");

        } catch (BusinessException | ResourceNotFoundException | ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ürün doğrulaması sırasında beklenmeyen hata: ", e);
            throw new ExternalServiceException("Sistemsel bir hata oluştu.", "UNEXPECTED_ERROR");
        }
    }
}