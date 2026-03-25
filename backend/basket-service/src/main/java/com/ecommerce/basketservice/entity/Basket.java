package com.ecommerce.basketservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "Basket", timeToLive = 604800)
public class Basket {

    @Id
    private UUID userId;

    @Builder.Default
    private List<BasketItem> items = new ArrayList<>();

    // Rich Domain Model
    public void addItem(BasketItem newItem) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        this.items.stream()
                .filter(item -> item.getProductId().equals(newItem.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existingItem -> existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity()),
                        () -> this.items.add(newItem)
                );
    }

    public BigDecimal calculateTotalPrice() {
        if (items == null || items.isEmpty()) return BigDecimal.ZERO;
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}