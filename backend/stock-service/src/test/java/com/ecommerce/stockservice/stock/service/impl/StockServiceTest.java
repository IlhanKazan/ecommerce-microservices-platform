package com.ecommerce.stockservice.stock.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.stockservice.client.adapter.ProductClientAdapter;
import com.ecommerce.stockservice.client.constants.ProductStatus;
import com.ecommerce.stockservice.client.dto.ProductResponse;
import com.ecommerce.stockservice.outbox.service.OutboxService;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.repository.StockRepository;
import com.ecommerce.stockservice.stockmovement.service.StockMovementService;
import com.ecommerce.stockservice.warehouse.entity.Warehouse;
import com.ecommerce.stockservice.warehouse.service.WarehouseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private ProductClientAdapter productClientAdapter;

    @Mock
    private OutboxService outboxService;

    @Mock
    private StockMovementService movementService;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    @DisplayName("Manuel stok ekleme: Ürün ilk kez ekleniyorsa (Upsert) yeni stok kaydı oluşmalı")
    void given_NewProduct_When_AddManualStock_Then_CreateNewStock() {

        Long tenantId = 1L;
        Long warehouseId = 10L;
        Long productId = 100L;
        int amount = 50;
        UUID userId = UUID.randomUUID();

        Warehouse warehouse = Warehouse.builder().build();
        ProductResponse product = new ProductResponse(productId, "SKU-123", ProductStatus.ACTIVE);


        when(warehouseService.findByTenantIdAndId(tenantId, warehouseId)).thenReturn(Optional.of(warehouse));
        when(productClientAdapter.validateAndGetProduct(productId, tenantId)).thenReturn(product);

        when(stockRepository.findByTenantIdAndWarehouseIdAndProductId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        when(stockRepository.save(any(Stock.class))).thenAnswer(invocation -> {
            Stock s = invocation.getArgument(0);
            s.setId(1001L);
            return s;
        });

        stockService.addManualStock(tenantId, warehouseId, productId, amount, userId);



        verify(stockRepository, times(1)).save(any(Stock.class));

        verify(movementService, times(1)).recordMovement(any(), any(), any(), eq(amount));

        verify(outboxService, times(1)).publishStockStatusChangedEvent(any(), eq(productId), eq(true), eq("RESTOCKED"));
    }

    @Test
    @DisplayName("Hatalı Depo: Depo bulunamazsa BusinessException fırlatılmalı")
    void given_InvalidWarehouse_When_AddManualStock_Then_ThrowBusinessException() {

        Long tenantId = 1L;
        Long warehouseId = 999L;

        when(warehouseService.findByTenantIdAndId(tenantId, warehouseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                stockService.addManualStock(tenantId, warehouseId, 100L, 50, UUID.randomUUID())
        )
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "WAREHOUSE_NOT_FOUND");

        verify(stockRepository, never()).save(any());
        verify(outboxService, never()).publishStockStatusChangedEvent(any(), anyLong(), anyBoolean(), anyString());
    }
}