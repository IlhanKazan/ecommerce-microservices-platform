package com.ecommerce.stockservice.stock.entity;

import com.ecommerce.common.entity.BaseEntity;
import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock extends BaseEntity {

    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    private Long productId;
    private String sku;

    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer lowStockThreshold;

    @Version
    private Long version;

    @Builder
    public Stock(Long tenantId, Warehouse warehouse, Long productId, String sku, Integer availableQuantity, Integer lowStockThreshold) {
        this.tenantId = tenantId;
        this.warehouse = warehouse;
        this.productId = productId;
        this.sku = sku;
        this.availableQuantity = availableQuantity != null ? availableQuantity : 0;
        this.reservedQuantity = 0;
        this.lowStockThreshold = lowStockThreshold != null ? lowStockThreshold : 5;
    }

    // Rich Domain Model
    public void reserve(int amount) {
        if (amount <= 0) throw new BusinessException("Miktar 0'dan büyük olmalı!", "INVALID_AMOUNT");
        if (this.availableQuantity < amount) {
            throw new BusinessException("Yetersiz stok!", "OUT_OF_STOCK");
        }
        this.availableQuantity -= amount;
        this.reservedQuantity += amount;
    }

    public void commitReservation(int amount) {
        if (this.reservedQuantity < amount) {
            throw new BusinessException("Rezerve edilenden fazla stok düşülemez!", "INVALID_COMMIT");
        }
        this.reservedQuantity -= amount;
    }

    public void rollbackReservation(int amount) {
        if (this.reservedQuantity < amount) {
            throw new BusinessException("İptal edilecek rezerve stok bulunamadı!", "INVALID_ROLLBACK");
        }
        this.reservedQuantity -= amount;
        this.availableQuantity += amount;
    }

    public void addStock(int amount) {
        if (amount <= 0) throw new BusinessException("Eklenecek stok 0'dan büyük olmalı!", "INVALID_AMOUNT");
        this.availableQuantity += amount;
    }
}