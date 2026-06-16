package com.ecommerce.productservice.common.buffer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Görüntülenme sayaçlarını Redis'te tampona alır (write-back/batching).
 * <p>
 * Her view'da DB'ye UPDATE atmak yerine ({@code products} hot-row contention + MVCC bloat),
 * Redis'te {@code INCR} ile sayılır; {@link com.ecommerce.productservice.product.scheduler.ViewCountFlushScheduler}
 * periyodik olarak delta'ları tek batch UPDATE ile DB'ye yazar. Sayaç non-kritik → best-effort.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ViewCountBuffer {

    private static final String COUNT_PREFIX = "views:count:";
    private static final String DIRTY_SET = "views:dirty";
    private static final int DRAIN_BATCH = 1000;

    private final StringRedisTemplate redis;

    /** View'ı tamponda say (DB'ye dokunmaz). Redis erişilemezse sessizce atlar. */
    public void record(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            redis.opsForValue().increment(COUNT_PREFIX + productId);
            redis.opsForSet().add(DIRTY_SET, productId.toString());
        } catch (Exception e) {
            log.warn("Görüntülenme tamponlanamadı (best-effort). ProductId: {}", productId, e);
        }
    }

    /**
     * Bekleyen delta'ları boşaltır: dirty set'ten id'leri SPOP ile atomik alır, her counter'ı
     * GETDEL (getAndDelete) ile okuyup siler. SPOP sonrası gelen view'lar id'yi tekrar ekler → sonraki tur yakalar.
     * @return productId → toplam delta (yalnız > 0)
     */
    public Map<Long, Long> drain() {
        Map<Long, Long> deltas = new HashMap<>();
        try {
            List<String> ids = redis.opsForSet().pop(DIRTY_SET, DRAIN_BATCH);
            if (ids == null || ids.isEmpty()) {
                return deltas;
            }
            for (String id : ids) {
                String raw = redis.opsForValue().getAndDelete(COUNT_PREFIX + id);
                if (raw == null) {
                    continue;
                }
                long delta = Long.parseLong(raw);
                if (delta > 0) {
                    deltas.put(Long.parseLong(id), delta);
                }
            }
        } catch (Exception e) {
            log.warn("Görüntülenme tamponu boşaltılamadı (best-effort).", e);
        }
        return deltas;
    }

    /** DB yazımı başarısızsa delta'yı tampona geri koyar (kayıp olmasın). */
    public void restore(Long productId, long delta) {
        try {
            redis.opsForValue().increment(COUNT_PREFIX + productId, delta);
            redis.opsForSet().add(DIRTY_SET, productId.toString());
        } catch (Exception e) {
            log.warn("Görüntülenme delta'sı geri konulamadı. ProductId: {}, delta: {}", productId, delta, e);
        }
    }
}
