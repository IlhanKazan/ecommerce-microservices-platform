package com.ecommerce.mailservice.mail.handler;

import com.ecommerce.contracts.event.order.OrderCancelledEventPayload;
import com.ecommerce.contracts.event.order.OrderConfirmedEventPayload;
import com.ecommerce.contracts.event.order.OrderDeliveredEventPayload;
import com.ecommerce.contracts.event.order.OrderRefundedEventPayload;
import com.ecommerce.contracts.event.order.OrderReturnRejectedEventPayload;
import com.ecommerce.contracts.event.order.OrderReturnRequestedEventPayload;
import com.ecommerce.contracts.event.order.OrderReturnedEventPayload;
import com.ecommerce.contracts.event.order.OrderShippedEventPayload;
import com.ecommerce.mailservice.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMailHandler {

    private final MailService mailService;

    public void handleOrderConfirmed(OrderConfirmedEventPayload payload, String messageId) {
        log.info("Sipariş onay maili — orderId: {}, email: {}", payload.orderId(), payload.recipientEmail());
        BigDecimal total = payload.items() == null ? BigDecimal.ZERO :
                payload.items().stream()
                        .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        mailService.sendOrderConfirmed(payload.recipientEmail(), payload.orderId(), total, "TRY", messageId);
    }

    public void handleOrderCancelled(OrderCancelledEventPayload payload, String messageId) {
        log.info("Sipariş iptal maili — orderId: {}", payload.orderId());
        mailService.sendOrderCancelled(payload.recipientEmail(), payload.orderId(), payload.reason(), messageId);
    }

    public void handleOrderShipped(OrderShippedEventPayload payload, String messageId) {
        log.info("Sipariş kargo maili — orderId: {}", payload.orderId());
        mailService.sendOrderShipped(payload.recipientEmail(), payload.orderId(), payload.trackingNumber(), messageId);
    }

    public void handleOrderRefunded(OrderRefundedEventPayload payload, String messageId) {
        log.info("Sipariş iade maili — orderId: {}", payload.orderId());
        mailService.sendOrderRefunded(payload.recipientEmail(), payload.orderId(), payload.reason(), messageId);
    }

    public void handleOrderDelivered(OrderDeliveredEventPayload payload, String messageId) {
        log.info("Sipariş teslim maili — orderId: {}, email: {}", payload.orderId(), payload.recipientEmail());
        mailService.sendOrderDelivered(payload.recipientEmail(), payload.orderId(), messageId);
    }

    public void handleOrderReturnRequested(OrderReturnRequestedEventPayload payload, String messageId) {
        log.info("İade talebi maili — orderId: {}", payload.orderId());
        mailService.sendOrderReturnRequested(payload.recipientEmail(), payload.orderId(), payload.reason(), messageId);
    }

    public void handleOrderReturnRejected(OrderReturnRejectedEventPayload payload, String messageId) {
        log.info("İade red maili — orderId: {}", payload.orderId());
        mailService.sendOrderReturnRejected(payload.recipientEmail(), payload.orderId(), payload.note(), messageId);
    }

    public void handleOrderReturned(OrderReturnedEventPayload payload, String messageId) {
        log.info("İade tamamlandı maili — orderId: {}", payload.orderId());
        mailService.sendOrderReturned(payload.recipientEmail(), payload.orderId(), payload.refundAmount(), messageId);
    }
}
