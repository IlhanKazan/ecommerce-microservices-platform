package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.dto.RefundRequest;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.orderservice.order.service.OrderCompensationService;
import com.ecommerce.orderservice.outbox.service.OutboxService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCompensationServiceImpl implements OrderCompensationService {

    private final OrderRepository orderRepository;
    private final OutboxService outboxService;
    private final PaymentServiceClient paymentClient;

    @Override
    @Transactional
    public void handleStockCommitFailed(Long orderId, String transactionId, String reason) {
        log.warn("[COMPENSATION] Stok commit başarısız! Ödeme iadesi başlatılıyor. OrderID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı: " + orderId, "ORDER_NOT_FOUND"));

        // Sipariş zaten iptal/iade edilmişse tekrar işleme
        if (order.getStatus() == com.ecommerce.orderservice.order.constant.OrderStatus.REFUNDED
                || order.getStatus() == com.ecommerce.orderservice.order.constant.OrderStatus.CANCELLED) {
            log.warn("[COMPENSATION] OrderID: {} zaten {} durumunda — işlem atlandı.", orderId, order.getStatus());
            return;
        }

        // İade dene — başarısız olursa loglayıp devam et; sipariş durumu yine REFUNDED olarak işaretlenir
        try {
            paymentClient.refundOrderPayment(new RefundRequest(orderId, transactionId));
            log.info("[COMPENSATION] Ödeme iadesi başarılı. OrderID: {}", orderId);
        } catch (FeignException e) {
            log.error("[COMPENSATION] Ödeme iadesi BAŞARISIZ — Manuel müdahale gerekli! OrderID: {}, Hata: {}",
                    orderId, e.getMessage());
            // İade başarısız olsa bile siparişi REFUNDED durumuna çek;
            // asıl iade takibi ödeme tarafında yapılmalı
        }

        // CONFIRMED → CANCELLED → REFUNDED (state machine kuralı)
        order.cancel("Stok tükendi - otomatik iptal");
        order.refund("Stok commit başarısız: " + reason);
        orderRepository.save(order);

        // ORDER_REFUNDED_EVENT outbox'a yaz — Propagation.MANDATORY, @Transactional içinde
        outboxService.publishOrderRefundedEvent(
                orderId, order.getUserId(), order.getTenantId(),
                "Stok commit başarısız: " + reason, order.getBuyerEmail());

        log.info("[COMPENSATION] OrderID: {} REFUNDED olarak işaretlendi.", orderId);
    }
}
