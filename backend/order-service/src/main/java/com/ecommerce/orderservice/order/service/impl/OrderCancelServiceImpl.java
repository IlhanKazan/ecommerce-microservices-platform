package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.dto.RefundRequest;
import com.ecommerce.orderservice.order.constant.OrderStatus;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderItem;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.orderservice.order.service.OrderCancelService;
import com.ecommerce.orderservice.outbox.service.OutboxService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancelServiceImpl implements OrderCancelService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OutboxService outboxService;
    private final PaymentServiceClient paymentClient;

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, UUID userId, String reason, String recipientEmail) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı.", "ORDER_NOT_FOUND"));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException(
                    "Sadece onaylanmış siparişler iptal edilebilir.", "INVALID_ORDER_STATUS");
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<OrderItemSnapshotPayload> snapshotItems = items.stream()
                .map(i -> new OrderItemSnapshotPayload(
                        i.getProductId(), i.getSku(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity()))
                .toList();

        // Ödeme iadesi — fire-and-forget (başarısız olsa da sipariş iptal edilir)
        try {
            paymentClient.refundOrderPayment(
                    new RefundRequest(orderId, order.getPaymentTransactionId()));
            log.info("[CANCEL] Ödeme iadesi başlatıldı. OrderID: {}", orderId);
        } catch (FeignException e) {
            log.error("[CANCEL] Ödeme iadesi başarısız — Manuel müdahale gerekli! OrderID: {}", orderId, e);
        }

        // CONFIRMED → CANCELLED → REFUNDED
        order.cancel(reason);
        order.refund(reason);
        orderRepository.save(order);

        // Outbox — Propagation.MANDATORY, bu @Transactional içinden çağrılıyor
        outboxService.publishOrderCancelledEvent(
                orderId, userId, order.getTenantId(), reason, snapshotItems, recipientEmail);
        outboxService.publishOrderRefundedEvent(
                orderId, userId, order.getTenantId(), reason, recipientEmail);

        log.info("[CANCEL] Sipariş iptal ve iade tamamlandı. OrderID: {}", orderId);
        return order;
    }
}
