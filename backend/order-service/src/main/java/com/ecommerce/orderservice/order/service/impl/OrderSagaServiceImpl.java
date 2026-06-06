package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.orderservice.client.BasketServiceClient;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.ProductServiceClient;
import com.ecommerce.orderservice.client.StockServiceClient;
import com.ecommerce.orderservice.client.UserTenantServiceClient;
import com.ecommerce.orderservice.client.dto.BasketResponse;
import com.ecommerce.orderservice.client.dto.OrderPaymentRequest;
import com.ecommerce.orderservice.client.dto.PaymentResult;
import com.ecommerce.orderservice.client.dto.ProductSnapshotInfo;
import com.ecommerce.orderservice.client.dto.StockReserveRequest;
import com.ecommerce.orderservice.order.command.CheckoutCommand;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.service.OrderPersistenceService;
import com.ecommerce.orderservice.order.service.OrderSagaService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderSagaServiceImpl implements OrderSagaService {

    private final BasketServiceClient basketClient;
    private final ProductServiceClient productClient;
    private final StockServiceClient stockClient;
    private final PaymentServiceClient paymentClient;
    private final OrderPersistenceService orderPersistenceService;
    private final UserTenantServiceClient userTenantClient;

    @Override
    public Order checkout(CheckoutCommand command) {

        // ─── 1. Basket ──────────────────────────────────────────────────
        BasketResponse basket;
        try {
            basket = basketClient.getMyBasket();
        } catch (FeignException e) {
            throw new ExternalServiceException("Sepet servisi yanıt vermiyor.", "BASKET_SERVICE_UNAVAILABLE");
        }

        if (basket.items() == null || basket.items().isEmpty()) {
            throw new BusinessException("Sepet boş!", "EMPTY_BASKET");
        }

        // ─── 2. Product snapshots ────────────────────────────────────────
        List<ProductSnapshotInfo> snapshots = new ArrayList<>();
        for (BasketResponse.BasketItemDto item : basket.items()) {
            try {
                ProductSnapshotInfo snapshot = productClient.getProductSnapshot(
                        item.productId(), command.tenantId());
                snapshots.add(snapshot);
            } catch (FeignException.NotFound e) {
                throw new BusinessException(
                        "Ürün bu mağazada bulunamadı. ProductId: " + item.productId(),
                        "PRODUCT_NOT_FOUND");
            } catch (FeignException.BadRequest e) {
                throw new BusinessException(
                        "Ürün satışta değil ya da stokta yok. ProductId: " + item.productId(),
                        "PRODUCT_NOT_AVAILABLE");
            } catch (FeignException e) {
                throw new ExternalServiceException("Ürün servisi yanıt vermiyor.", "PRODUCT_SERVICE_UNAVAILABLE");
            }
        }

        // Toplam tutarı snapshot fiyatlarından hesapla — frontend/basket fiyatına güvenilmez
        BigDecimal totalAmount = calculateTotal(basket.items(), snapshots);
        String currency = snapshots.get(0).currency();

        // ─── 3. Stok rezervasyonu ────────────────────────────────────────
        String tempOrderId = "CHECKOUT-" + UUID.randomUUID();
        StockReserveRequest stockReq = new StockReserveRequest(
                tempOrderId,
                command.tenantId(),
                basket.items().stream()
                        .map(i -> new StockReserveRequest.StockItemDto(i.productId(), i.quantity()))
                        .toList()
        );
        reserveStockOrThrow(stockReq);

        // ─── 4. Ödeme ────────────────────────────────────────────────────
        String subMerchantKey = fetchSubMerchantKeyQuietly(command.tenantId());

        OrderPaymentRequest paymentReq = new OrderPaymentRequest(
                null,
                command.tenantId(),
                totalAmount,
                currency,
                command.cardInfo(),
                command.buyer(),
                command.billingAddress(),
                parseShippingAddress(command.shippingAddressJson(), command.billingAddress()),
                subMerchantKey
        );
        PaymentResult paymentResult = processPaymentOrRollback(paymentReq, stockReq);

        // ─── 5. Order + items + outbox — tek @Transactional ─────────────
        Order order = orderPersistenceService.saveOrderWithItems(
                command, basket, snapshots, totalAmount, currency, paymentResult);

        // ─── 6. Basket temizle — fire-and-forget ─────────────────────────
        clearBasketQuietly();

        return order;
    }

    private void reserveStockOrThrow(StockReserveRequest req) {
        try {
            stockClient.reserveStock(req);
        } catch (FeignException.Conflict e) {
            throw new BusinessException("Yetersiz stok. Lütfen miktarı azaltın.", "INSUFFICIENT_STOCK");
        } catch (FeignException.BadRequest e) {
            throw new BusinessException("Stok rezervasyonu başarısız.", "STOCK_RESERVE_FAILED");
        } catch (FeignException e) {
            throw new ExternalServiceException("Stok servisi yanıt vermiyor.", "STOCK_SERVICE_UNAVAILABLE");
        }
    }

    private PaymentResult processPaymentOrRollback(OrderPaymentRequest req, StockReserveRequest stockReq) {
        PaymentResult result;
        try {
            result = paymentClient.processOrderPayment(req);
        } catch (FeignException e) {
            log.error("Ödeme servisi hata! Status: {}, Body: {}, TempOrderID: {}",
                    e.status(), e.contentUTF8(), stockReq.orderId());
            rollbackStockQuietly(stockReq);
            throw new ExternalServiceException("Ödeme servisi yanıt vermiyor.", "PAYMENT_SERVICE_UNAVAILABLE");
        }
        if (!result.success()) {
            log.warn("Ödeme başarısız: {}. Stok geri alınıyor.", result.failureReason());
            rollbackStockQuietly(stockReq);
            throw new BusinessException("Ödeme başarısız: " + result.failureReason(), "PAYMENT_FAILED");
        }
        return result;
    }

    private void rollbackStockQuietly(StockReserveRequest req) {
        try {
            stockClient.rollbackStock(req);
        } catch (Exception e) {
            log.error("STOK ROLLBACK BAŞARISIZ — Manuel müdahale gerekli! TempOrderID: {}", req.orderId(), e);
        }
    }

    private void clearBasketQuietly() {
        try {
            basketClient.clearMyBasket();
        } catch (Exception e) {
            log.warn("Sepet temizlenemedi (önemsiz, sipariş tamamlandı): {}", e.getMessage());
        }
    }

    private BigDecimal calculateTotal(List<BasketResponse.BasketItemDto> items,
                                      List<ProductSnapshotInfo> snapshots) {
        Map<Long, BigDecimal> priceMap = snapshots.stream()
                .collect(Collectors.toMap(ProductSnapshotInfo::id, ProductSnapshotInfo::price));
        return items.stream()
                .map(item -> {
                    BigDecimal price = priceMap.getOrDefault(item.productId(), item.price());
                    return price.multiply(BigDecimal.valueOf(item.quantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String fetchSubMerchantKeyQuietly(Long tenantId) {
        try {
            return userTenantClient.getSubMerchantKey(tenantId);
        } catch (Exception e) {
            log.warn("SubMerchantKey alınamadı (tenantId={}): {}", tenantId, e.getMessage());
            return null;
        }
    }

    private OrderPaymentRequest.AddressDto parseShippingAddress(
            String shippingAddressJson, OrderPaymentRequest.AddressDto fallback) {
        // shippingAddressJson ileride gerçek parse eklenebilir;
        // şu an billingAddress'i shipping olarak kullanıyoruz
        if (shippingAddressJson == null || shippingAddressJson.isBlank()) {
            return fallback;
        }
        return fallback;
    }
}
