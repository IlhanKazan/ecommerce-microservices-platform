package com.ecommerce.basketservice.service;

import com.ecommerce.basketservice.entity.Basket;
import com.ecommerce.basketservice.entity.BasketItem;
import com.ecommerce.basketservice.repository.BasketRepository;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /**
     * Guest sepetini hesap sepetiyle birleştirir — TEK kilit altında, atomik.
     * Her item için mevcut basket.addItem (aynı üründe miktar toplanır) uygulanır; tek save.
     * Item'lar zaten product-service ile enrich edilmiş (taze fiyat) gelir.
     */
    public Basket mergeItems(UUID userId, List<BasketItem> items) {
        String lockKey = "lock:basket:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                Basket basket = basketRepository.findById(String.valueOf(userId))
                        .orElse(Basket.builder().userId(userId).build());

                for (BasketItem item : items) {
                    basket.addItem(item);
                }

                basketRepository.save(basket);
                log.info("Guest sepeti birleştirildi. UserID: {}, item sayısı: {}", userId, items.size());
                return basket;
            } else {
                throw new BusinessException("Sepetiniz şu an güncelleniyor, lütfen bekleyip tekrar deneyin.", "BASKET_LOCKED");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Sistem hatası: Kilit beklenirken kesilme oldu.", "LOCK_INTERRUPTED");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public void removeItemFromBasket(UUID userId, Long productId) {
        String lockKey = "lock:basket:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                log.debug("Kilit alındı: {}. Ürün kaldırılıyor...", lockKey);
                Basket basket = basketRepository.findById(String.valueOf(userId))
                        .orElseThrow(() -> new ResourceNotFoundException("Sepet bulunamadı.", "BASKET_NOT_FOUND"));
                basket.removeItem(productId);
                basketRepository.save(basket);
                log.debug("Ürün sepetten kaldırıldı. UserID: {}, ProductID: {}", userId, productId);
            } else {
                log.warn("Kilit alınamadı: {}", lockKey);
                throw new BusinessException("Sepetiniz şu an güncelleniyor, lütfen bekleyip tekrar deneyin.", "BASKET_LOCKED");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Sistem hatası: Kilit beklenirken kesilme oldu.", "LOCK_INTERRUPTED");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Kilit bırakıldı: {}", lockKey);
            }
        }
    }

    public void setItemQuantity(UUID userId, Long productId, int newQuantity) {
        String lockKey = "lock:basket:" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                log.debug("Kilit alındı: {}. Miktar güncelleniyor...", lockKey);
                Basket basket = basketRepository.findById(String.valueOf(userId))
                        .orElseThrow(() -> new ResourceNotFoundException("Sepet bulunamadı.", "BASKET_NOT_FOUND"));
                basket.setItemQuantity(productId, newQuantity);
                basketRepository.save(basket);
                log.debug("Ürün miktarı güncellendi. UserID: {}, ProductID: {}, NewQty: {}", userId, productId, newQuantity);
            } else {
                log.warn("Kilit alınamadı: {}", lockKey);
                throw new BusinessException("Sepetiniz şu an güncelleniyor, lütfen bekleyip tekrar deneyin.", "BASKET_LOCKED");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Sistem hatası: Kilit beklenirken kesilme oldu.", "LOCK_INTERRUPTED");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Kilit bırakıldı: {}", lockKey);
            }
        }
    }

    public Basket getBasket(UUID userId) {
        return basketRepository.findById(String.valueOf(userId))
                .map(basket -> {
                    basketRepository.save(basket);
                    return basket;
                })
                .orElse(Basket.builder().userId(userId).build());
    }

    public void deleteBasket(UUID userId) {
        basketRepository.deleteById(String.valueOf(userId));
    }
}