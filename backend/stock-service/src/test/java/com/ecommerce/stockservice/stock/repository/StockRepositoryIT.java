package com.ecommerce.stockservice.stock.repository;

import com.ecommerce.stockservice.base.AbstractBaseIntegrationTest;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class StockRepositoryIT extends AbstractBaseIntegrationTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Pessimistic Lock: 10 kişi aynı anda 1 stoğu almaya çalışırsa sadece 1 kişi alabilmeli")
    void given_OneStock_When_TenThreadsTryToReserve_Then_OnlyOneShouldSucceed() throws InterruptedException {

        Warehouse warehouse = warehouseRepository.saveAndFlush(
                Warehouse.builder().tenantId(1L).code("TEST-01").name("Test Depo").isActive(true).build()
        );

        Stock stock = stockRepository.saveAndFlush(
                Stock.builder()
                        .tenantId(1L)
                        .warehouse(warehouse)
                        .productId(100L)
                        .sku("IPHONE-15")
                        .availableQuantity(1)
                        .build()
        );


        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successfulReserves = new AtomicInteger(0);
        AtomicInteger failedReserves = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    transactionTemplate.executeWithoutResult(status -> {

                        Stock lockedStock = stockRepository.findWithLockingByTenantIdAndWarehouseIdAndProductId(
                                1L, warehouse.getId(), 100L
                        ).orElseThrow();

                        lockedStock.reserve(1);
                        stockRepository.saveAndFlush(lockedStock);
                        successfulReserves.incrementAndGet();
                    });
                } catch (Exception e) {
                    failedReserves.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();

        assertThat(successfulReserves.get()).isEqualTo(1);
        assertThat(failedReserves.get()).isEqualTo(9);

        Stock finalStock = stockRepository.findById(stock.getId()).orElseThrow();
        assertThat(finalStock.getAvailableQuantity()).isZero();
    }
}