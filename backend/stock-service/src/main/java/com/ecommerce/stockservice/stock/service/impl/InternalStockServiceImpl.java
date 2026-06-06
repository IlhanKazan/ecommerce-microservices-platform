package com.ecommerce.stockservice.stock.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;
import com.ecommerce.stockservice.outbox.constant.TransactionType;
import com.ecommerce.stockservice.outbox.service.OutboxService;
import com.ecommerce.stockservice.stock.controller.dto.request.InternalStockReserveRequest.StockItemRequest;
import com.ecommerce.stockservice.stock.entity.Stock;
import com.ecommerce.stockservice.stock.repository.StockRepository;
import com.ecommerce.stockservice.stock.service.InternalStockService;
import com.ecommerce.stockservice.stockmovement.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalStockServiceImpl implements InternalStockService {

    private final StockRepository stockRepository;
    private final OutboxService outboxService;
    private final StockMovementService movementService;

    @Override
    @Transactional
    public void reserveAllForOrder(String orderId, Long tenantId, List<StockItemRequest> items) {
        log.info("[INTERNAL] Sipariş için toplu rezervasyon. OrderID: {}, ItemCount: {}", orderId, items.size());
        for (StockItemRequest item : items) {
            List<Stock> candidates = stockRepository.findWithSufficientStockLocked(
                    tenantId, item.productId(), item.quantity());
            if (candidates.isEmpty()) {
                throw new BusinessException(
                        "Ürün için yeterli stok yok. ProductId: " + item.productId(),
                        "OUT_OF_STOCK");
            }
            Stock stock = candidates.get(0);
            int oldQty = stock.getAvailableQuantity();
            stock.reserve(item.quantity());

            if (oldQty > 0 && stock.getAvailableQuantity() == 0) {
                outboxService.publishStockStatusChangedEvent(
                        stock.getId().toString(), item.productId(), false, "OUT_OF_STOCK");
            }
            movementService.recordMovement(stock, TransactionType.RESERVED_FOR_ORDER, orderId, -item.quantity());
            outboxService.publishStockReservedEvent(
                    stock.getId().toString(), item.productId(), item.quantity(), orderId);
            stockRepository.save(stock);
        }
        log.info("[INTERNAL] Toplu rezervasyon tamamlandı. OrderID: {}", orderId);
    }

    @Override
    @Transactional
    public void rollbackAllForOrder(String orderId, Long tenantId, List<StockItemRequest> items) {
        log.info("[INTERNAL] Sipariş stok rollback. OrderID: {}", orderId);
        for (StockItemRequest item : items) {
            stockRepository.findAllByTenantIdAndProductId(tenantId, item.productId())
                    .stream()
                    .filter(s -> s.getReservedQuantity() >= item.quantity())
                    .findFirst()
                    .ifPresentOrElse(stock -> {
                        stock.rollbackReservation(item.quantity());
                        movementService.recordMovement(
                                stock, TransactionType.ROLLBACK_RESERVATION, orderId, item.quantity());
                        stockRepository.save(stock);
                        log.info("[INTERNAL] Rollback tamamlandı. ProductId: {}, Miktar: {}",
                                item.productId(), item.quantity());
                    }, () -> log.warn("[INTERNAL] Rollback: Rezerve stok bulunamadı. Product: {}, OrderID: {}",
                            item.productId(), orderId));
        }
    }

    @Override
    @Transactional
    public void commitAllForOrder(String orderId, Long tenantId, List<OrderItemSnapshotPayload> items) {
        log.info("[INTERNAL] Sipariş stok commit. OrderID: {}", orderId);
        for (OrderItemSnapshotPayload item : items) {
            stockRepository.findAllByTenantIdAndProductId(tenantId, item.productId())
                    .stream()
                    .filter(s -> s.getReservedQuantity() >= item.quantity())
                    .findFirst()
                    .ifPresentOrElse(stock -> {
                        stock.commitReservation(item.quantity());
                        movementService.recordMovement(stock, TransactionType.ORDER_COMPLETED, orderId, -item.quantity());
                        stockRepository.save(stock);
                        log.info("[COMMIT] ProductId: {}, Miktar: {}", item.productId(), item.quantity());
                    }, () -> log.warn("[COMMIT] Rezerve stok bulunamadı. ProductId: {}, OrderID: {}",
                            item.productId(), orderId));
        }
    }

    @Override
    @Transactional
    public void cancelAllForOrder(String orderId, Long tenantId, List<OrderItemSnapshotPayload> items) {
        log.info("[INTERNAL] Sipariş iptal stok rollback. OrderID: {}", orderId);
        for (OrderItemSnapshotPayload item : items) {
            stockRepository.findAllByTenantIdAndProductId(tenantId, item.productId())
                    .stream()
                    .filter(s -> s.getReservedQuantity() >= item.quantity())
                    .findFirst()
                    .ifPresentOrElse(stock -> {
                        stock.rollbackReservation(item.quantity());
                        movementService.recordMovement(stock, TransactionType.ROLLBACK_RESERVATION, orderId, item.quantity());
                        stockRepository.save(stock);
                        log.info("[CANCEL] Rollback tamamlandı. ProductId: {}, Miktar: {}", item.productId(), item.quantity());
                    }, () -> log.warn("[CANCEL] Rezerve stok bulunamadı. ProductId: {}, OrderID: {}",
                            item.productId(), orderId));
        }
    }
}
