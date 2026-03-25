package com.ecommerce.stockservice.stock.entity;

import com.ecommerce.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockDomainTest {

    @Test
    @DisplayName("Stok yeterliyse rezervasyon işlemi başarılı olmalı ve miktarı düşmeli")
    void given_EnoughStock_When_Reserve_Then_AvailableQuantityShouldDecrease() {

        Stock stock = Stock.builder()
                .availableQuantity(10)
                .build();

        stock.reserve(3);

        assertThat(stock.getAvailableQuantity()).isEqualTo(7);
    }

    @Test
    @DisplayName("Mevcut stoktan fazla rezervasyon yapılmaya çalışıldığında BusinessException fırlatılmalı")
    void given_NotEnoughStock_When_Reserve_Then_ThrowBusinessException() {

        Stock stock = Stock.builder()
                .availableQuantity(5)
                .build();

        assertThatThrownBy(() -> stock.reserve(10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Yetersiz stok")
                .hasFieldOrPropertyWithValue("errorCode", "OUT_OF_STOCK");
    }

    @Test
    @DisplayName("Manuel stok eklendiğinde mevcut stok miktarı artmalı")
    void given_ValidAmount_When_AddStock_Then_AvailableQuantityShouldIncrease() {

        Stock stock = Stock.builder()
                .availableQuantity(20)
                .build();

        stock.addStock(15);

        assertThat(stock.getAvailableQuantity()).isEqualTo(35);
    }
}