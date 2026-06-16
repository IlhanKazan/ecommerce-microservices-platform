package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;
import com.ecommerce.orderservice.client.dto.BasketResponse;
import com.ecommerce.orderservice.client.dto.PaymentResult;
import com.ecommerce.orderservice.client.dto.ProductSnapshotInfo;
import com.ecommerce.orderservice.order.command.CheckoutCommand;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderItem;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.orderservice.order.service.OrderPersistenceService;
import com.ecommerce.orderservice.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPersistenceServiceImpl implements OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public Order saveOrderWithItems(
            CheckoutCommand command,
            BasketResponse basket,
            List<ProductSnapshotInfo> snapshots,
            BigDecimal totalAmount,
            String currency,
            PaymentResult paymentResult) {

        Order order = Order.builder()
                .userId(command.userId())
                .tenantId(command.tenantId())
                .totalAmount(totalAmount)
                .currency(currency)
                .shippingAddressJson(command.shippingAddressJson())
                .paymentTransactionId(paymentResult.transactionId())
                .commissionAmount(paymentResult.commissionAmount())
                .buyerEmail(command.recipientEmail())
                .build();
        order = orderRepository.save(order);

        Map<Long, ProductSnapshotInfo> snapshotMap = snapshots.stream()
                .collect(Collectors.toMap(ProductSnapshotInfo::id, s -> s));

        final Long orderId = order.getId();
        List<OrderItem> items = basket.items().stream()
                .map(basketItem -> {
                    ProductSnapshotInfo snap = snapshotMap.get(basketItem.productId());
                    return OrderItem.builder()
                            .orderId(orderId)
                            .tenantId(command.tenantId())
                            .productId(basketItem.productId())
                            .sku(snap != null ? snap.sku() : "")
                            .productName(snap != null ? snap.name() : basketItem.productName())
                            .productImageUrl(snap != null ? snap.mainImageUrl() : basketItem.imageUrl())
                            .unitPrice(snap != null ? snap.price() : basketItem.price())
                            .quantity(basketItem.quantity())
                            .build();
                })
                .toList();
        orderItemRepository.saveAll(items);

        // OrderItem snapshot listesi — stok commit event'ine taşınacak
        List<OrderItemSnapshotPayload> snapshotItems = items.stream()
                .map(i -> new OrderItemSnapshotPayload(
                        i.getProductId(), i.getSku(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();

        // OutboxService.publishOrderConfirmedEvent — Propagation.MANDATORY, bu @Transactional içinden çağrılıyor
        outboxService.publishOrderConfirmedEvent(orderId, command.userId(), command.tenantId(), snapshotItems, command.recipientEmail());

        log.info("Sipariş kaydedildi ve ORDER_CONFIRMED_EVENT outbox'a yazıldı. OrderID: {}", orderId);
        return order;
    }
}
