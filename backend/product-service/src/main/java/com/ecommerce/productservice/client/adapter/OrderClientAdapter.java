package com.ecommerce.productservice.client.adapter;

import com.ecommerce.productservice.client.OrderServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderClientAdapter {

    private final OrderServiceClient orderServiceClient;

    /**
     * Kullanıcının belirtilen ürünü satın alıp almadığını sorgular.
     * Fail-open: order-service erişilemez durumdaysa yorum engellenmez (true döner).
     */
    public boolean hasPurchased(UUID userId, Long productId) {
        try {
            Boolean result = orderServiceClient.hasPurchased(userId, productId);
            return Boolean.TRUE.equals(result);
        } catch (FeignException e) {
            log.warn("Order-service'e erişilemedi, satın alma kontrolü atlandı (userId={}, productId={}): {}",
                    userId, productId, e.getMessage());
            return true;
        }
    }
}
