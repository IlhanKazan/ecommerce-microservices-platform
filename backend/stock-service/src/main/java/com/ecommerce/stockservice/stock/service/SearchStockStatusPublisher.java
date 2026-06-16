package com.ecommerce.stockservice.stock.service;

import com.ecommerce.stockservice.client.adapter.ProductClientAdapter;
import com.ecommerce.stockservice.client.dto.StockGroupResponse;
import com.ecommerce.stockservice.outbox.service.OutboxService;
import com.ecommerce.stockservice.stock.repository.ProductStockSumProjection;
import com.ecommerce.stockservice.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stoğu değişen ürün için ES {@code inStock} durumunu doğru hedefe (parent/standalone) yayınlar.
 * <p>
 * Search index'inde yalnızca parent/standalone ürünler yer alır. Bir varyantın stoğu değiştiğinde
 * güncellenmesi gereken doküman parent'tır ve parent'ın {@code inStock} değeri tüm aktif varyantlarının
 * stoğunun VEYA'sıdır. Bu publisher hedefi product-service'ten çözer, üyelerin toplam stoğunu yerel
 * DB'den hesaplar ve outbox event'ini parent id'siyle yazar.
 * <p>
 * Yalnızca stok 0 sınırını geçen işlemlerde çağrılır (her stok değişiminde değil) — product-service
 * çağrısı bu yüzden nadirdir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchStockStatusPublisher {

    private final ProductClientAdapter productClientAdapter;
    private final StockRepository stockRepository;
    private final OutboxService outboxService;

    /**
     * @param changedProductId   stoğu değişen ürün (parent veya varyant) id'si
     * @param outboxAggregateId  outbox/Debezium routing anahtarı (genelde değişen stok kaydının id'si)
     */
    public void publish(Long changedProductId, String outboxAggregateId) {
        StockGroupResponse group = productClientAdapter.resolveStockGroup(changedProductId);

        long total = stockRepository.sumAvailableByProductIds(group.memberIds()).stream()
                .mapToLong(p -> p.getQty() == null ? 0L : p.getQty())
                .sum();
        boolean inStock = total > 0;

        outboxService.publishStockStatusChangedEvent(
                outboxAggregateId,
                group.searchTargetId(),
                inStock,
                inStock ? "IN_STOCK" : "OUT_OF_STOCK");

        log.info("ES stok durumu yayınlandı. Hedef: {}, inStock: {} (değişen ürün: {})",
                group.searchTargetId(), inStock, changedProductId);
    }
}
