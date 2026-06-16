package com.ecommerce.productservice.product.scheduler;

import com.ecommerce.productservice.outbox.service.OutboxService;
import com.ecommerce.productservice.product.entity.Product;
import com.ecommerce.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Popülerlik sayaçlarını (view_count/sale_count) periyodik olarak ES'e taşır.
 * <p>
 * Sayaçlar product DB'de yüksek frekansla artar (her görüntülenme/satış); her artışta event yaymak
 * yerine bu job belirli aralıkla, sayacı değişmiş ürünler için tek bir {@code PRODUCT_STATS_CHANGED}
 * event'i yayar. search-service bunu ES dokümanına partial update olarak uygular ("popular" sıralaması).
 * <p>
 * MVP: sayacı &gt; 0 olan tüm ana ürünler republish edilir (ES idempotent overwrite). İleride
 * "dirty" işaretiyle yalnızca değişenlere indirgenebilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductStatsSyncScheduler {

    private final ProductRepository productRepository;
    private final OutboxService outboxService;

    @Scheduled(fixedDelayString = "${app.stats-sync.delay-ms:600000}", initialDelayString = "${app.stats-sync.initial-delay-ms:60000}")
    @Transactional
    public void syncProductStats() {
        // Yalnızca sayacı son senkrondan beri DEĞİŞEN ürünler — tüm katalog elden geçirilmez.
        List<Product> products = productRepository.findDirtyStatsProducts();
        if (products.isEmpty()) {
            return;
        }
        for (Product product : products) {
            outboxService.publishProductStatsChangedEvent(product);
        }
        // Yayınlananların dirty işaretini temizle (sonraki tick'te tekrar yayılmasın).
        productRepository.clearStatsDirty(products.stream().map(Product::getId).toList());
        log.info("[STATS-SYNC] {} dirty ürün için popülerlik event'i yayınlandı.", products.size());
    }
}
