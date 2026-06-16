package com.ecommerce.productservice.product.scheduler;

import com.ecommerce.productservice.common.buffer.ViewCountBuffer;
import com.ecommerce.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Redis'te tamponlanan görüntülenme delta'larını periyodik olarak DB'ye batch'ler (write-back).
 * Her view'da DB UPDATE yerine ürün başına ~periyotta bir UPDATE → hot-row contention/MVCC bloat biter.
 * UPDATE {@code stats_dirty=true} set ettiği için ProductStatsSyncScheduler view_count'u ES'e taşımaya devam eder.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountFlushScheduler {

    private final ViewCountBuffer viewCountBuffer;
    private final ProductRepository productRepository;

    @Scheduled(fixedDelayString = "${app.view-flush.delay-ms:30000}",
            initialDelayString = "${app.view-flush.initial-delay-ms:30000}")
    @Transactional
    public void flush() {
        Map<Long, Long> deltas = viewCountBuffer.drain();
        if (deltas.isEmpty()) {
            return;
        }
        int applied = 0;
        for (Map.Entry<Long, Long> e : deltas.entrySet()) {
            try {
                productRepository.incrementViewCountBy(e.getKey(), e.getValue());
                applied++;
            } catch (Exception ex) {
                // DB yazımı patlarsa delta'yı tampona geri koy (kayıp olmasın).
                viewCountBuffer.restore(e.getKey(), e.getValue());
                log.warn("View delta DB'ye yazılamadı, geri kondu. ProductId: {}, delta: {}",
                        e.getKey(), e.getValue(), ex);
            }
        }
        log.info("[VIEW-FLUSH] {} ürün için görüntülenme deltası DB'ye yazıldı.", applied);
    }
}
