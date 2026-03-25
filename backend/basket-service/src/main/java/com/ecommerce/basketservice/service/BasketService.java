package com.ecommerce.basketservice.service;

import com.ecommerce.basketservice.entity.Basket;
import com.ecommerce.basketservice.entity.BasketItem;
import com.ecommerce.basketservice.repository.BasketRepository;
import com.ecommerce.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BasketService {

    private final BasketRepository basketRepository;
    private final RedissonClient redissonClient;

    public void addItemToBasket(UUID userId, BasketItem newItem) {
        String lockKey = "lock:basket:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                log.debug("Kilit alındı: {}. Sepet güncelleniyor...", lockKey);

                Basket basket = basketRepository.findById(String.valueOf(userId))
                        .orElse(Basket.builder().userId(userId).build());

                basket.addItem(newItem);

                basketRepository.save(basket);
                log.debug("Sepet başarıyla kaydedildi: UserID {}", userId);
            } else {
                log.warn("Kilit alınamadı! Başka bir thread şu an işlem yapıyor: {}", lockKey);
                throw new BusinessException("Sepetiniz şu an güncelleniyor, lütfen biraz bekleyip tekrar deneyin.", "BASKET_LOCKED");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Sistem hatası: Kilit beklenirken kesilme oldu.", "LOCK_INTERRUPTED");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("Kilit bırakıldı: {}", lockKey);
            }
        }
    }

    public Basket getBasket(UUID userId) {
        return basketRepository.findById(String.valueOf(userId))
                .orElse(Basket.builder().userId(userId).build());
    }

    public void deleteBasket(UUID userId) {
        basketRepository.deleteById(String.valueOf(userId));
    }
}