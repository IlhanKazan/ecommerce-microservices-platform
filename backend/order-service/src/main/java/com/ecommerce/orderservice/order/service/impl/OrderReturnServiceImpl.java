package com.ecommerce.orderservice.order.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ExternalServiceException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.contracts.event.order.OrderItemSnapshotPayload;
import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.client.dto.RefundRequest;
import com.ecommerce.orderservice.client.dto.RefundResponse;
import com.ecommerce.orderservice.order.constant.ReturnStatus;
import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.entity.OrderReturn;
import com.ecommerce.orderservice.order.query.ReturnInfo;
import com.ecommerce.orderservice.order.repository.OrderItemRepository;
import com.ecommerce.orderservice.order.repository.OrderRepository;
import com.ecommerce.orderservice.order.repository.OrderReturnRepository;
import com.ecommerce.orderservice.order.service.OrderReturnService;
import com.ecommerce.orderservice.outbox.service.OutboxService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderReturnServiceImpl implements OrderReturnService {

    // İade penceresi: teslim tarihinden itibaren bu kadar gün içinde talep açılabilir (TR cayma hakkı standardı)
    private static final int RETURN_WINDOW_DAYS = 14;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReturnRepository returnRepository;
    private final OutboxService outboxService;
    private final PaymentServiceClient paymentClient;

    @Override
    @Transactional
    public Order requestReturn(Long orderId, UUID userId, String reasonCode, String note) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı.", "ORDER_NOT_FOUND"));

        // 14 günlük iade penceresi — teslim tarihi varsa kontrol et (eski siparişlerde deliveredAt null → grace)
        if (order.getDeliveredAt() != null
                && order.getDeliveredAt().plusDays(RETURN_WINDOW_DAYS).isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "İade süresi (" + RETURN_WINDOW_DAYS + " gün) dolmuş.", "RETURN_WINDOW_EXPIRED");
        }

        returnRepository.findByOrderIdAndStatus(orderId, ReturnStatus.REQUESTED).ifPresent(r -> {
            throw new BusinessException("Bu sipariş için zaten açık bir iade talebi var.", "RETURN_ALREADY_REQUESTED");
        });

        // order.requestReturn() yalnız DELIVERED'da çalışır; RETURN_REJECTED terminal olduğundan tekrar talep engellenir
        order.requestReturn(); // DELIVERED → RETURN_REQUESTED
        OrderReturn ret = OrderReturn.builder()
                .orderId(orderId).userId(userId).tenantId(order.getTenantId())
                .reasonCode(reasonCode).reason(note)
                .build();
        returnRepository.save(ret);
        orderRepository.save(order);

        // Mail için okunur sebep: not varsa not, yoksa kod
        String displayReason = (note != null && !note.isBlank()) ? note : reasonCode;
        outboxService.publishOrderReturnRequestedEvent(
                orderId, userId, order.getTenantId(), displayReason, order.getBuyerEmail());
        log.info("[RETURN] İade talebi açıldı. OrderID: {}, sebep: {}, byUser: {}", orderId, reasonCode, userId);
        return order;
    }

    @Override
    @Transactional
    public Order approveReturn(Long orderId, Long tenantId, String note) {
        boolean byAdmin = (tenantId == null);
        Order order = loadOrder(orderId, tenantId);
        OrderReturn ret = openReturn(orderId);

        // Gerçek iyzico Refund — başarısızsa durum DEĞİŞMEZ, merchant/admin tekrar dener (çift refund yok)
        RefundResponse refund;
        try {
            refund = paymentClient.refundOrderPayment(
                    new RefundRequest(orderId, order.getPaymentTransactionId(), order.getTotalAmount(), "REFUND"));
        } catch (FeignException e) {
            log.error("[RETURN] İade ödemesi çağrısı başarısız. OrderID: {}", orderId, e);
            throw new ExternalServiceException("İade ödemesi başlatılamadı, lütfen tekrar deneyin.", "REFUND_CALL_FAILED");
        }
        if (refund == null || !refund.success()) {
            String msg = refund != null ? refund.message() : "yanıt yok";
            log.error("[RETURN] iyzico iade reddetti. OrderID: {}, mesaj: {}", orderId, msg);
            throw new BusinessException("İade ödemesi başarısız: " + msg, "REFUND_FAILED");
        }

        order.approveReturn(); // RETURN_REQUESTED → RETURNED
        ret.approve(note, byAdmin, order.getTotalAmount(), true);
        returnRepository.save(ret);
        orderRepository.save(order);

        outboxService.publishOrderReturnedEvent(
                orderId, order.getUserId(), order.getTenantId(),
                snapshotItems(orderId), order.getTotalAmount(), order.getBuyerEmail());
        log.info("[RETURN] İade onaylandı + para iadesi yapıldı. OrderID: {}, byAdmin: {}", orderId, byAdmin);
        return order;
    }

    @Override
    @Transactional
    public Order rejectReturn(Long orderId, Long tenantId, String note) {
        boolean byAdmin = (tenantId == null);
        Order order = loadOrder(orderId, tenantId);
        OrderReturn ret = openReturn(orderId);

        order.rejectReturn(); // RETURN_REQUESTED → DELIVERED
        ret.reject(note, byAdmin);
        returnRepository.save(ret);
        orderRepository.save(order);

        outboxService.publishOrderReturnRejectedEvent(
                orderId, order.getUserId(), order.getTenantId(), note, order.getBuyerEmail());
        log.info("[RETURN] İade reddedildi. OrderID: {}, byAdmin: {}", orderId, byAdmin);
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnInfo> getTenantReturns(Long tenantId) {
        return toInfos(returnRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, ReturnStatus.REQUESTED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnInfo> getAllReturns() {
        return toInfos(returnRepository.findByStatusOrderByCreatedAtDesc(ReturnStatus.REQUESTED));
    }

    // ─── helpers ───
    private Order loadOrder(Long orderId, Long tenantId) {
        if (tenantId != null) {
            return orderRepository.findByIdAndTenantId(orderId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sipariş bu mağazada bulunamadı.", "ORDER_NOT_FOUND"));
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sipariş bulunamadı.", "ORDER_NOT_FOUND"));
    }

    private OrderReturn openReturn(Long orderId) {
        return returnRepository.findByOrderIdAndStatus(orderId, ReturnStatus.REQUESTED)
                .orElseThrow(() -> new BusinessException("Açık iade talebi bulunamadı.", "RETURN_NOT_FOUND"));
    }

    private List<OrderItemSnapshotPayload> snapshotItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(i -> new OrderItemSnapshotPayload(
                        i.getProductId(), i.getSku(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                .toList();
    }

    private List<ReturnInfo> toInfos(List<OrderReturn> returns) {
        return returns.stream().map(r -> {
            Order o = orderRepository.findById(r.getOrderId()).orElse(null);
            return new ReturnInfo(
                    r.getId(), r.getOrderId(), r.getTenantId(),
                    r.getReasonCode(), r.getReason(), r.getStatus().name(),
                    o != null ? o.getTotalAmount() : null,
                    o != null ? o.getBuyerEmail() : null,
                    r.getCreatedAt());
        }).toList();
    }
}
