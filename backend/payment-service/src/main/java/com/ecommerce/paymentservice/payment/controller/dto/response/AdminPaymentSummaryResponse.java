package com.ecommerce.paymentservice.payment.controller.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Platform admin ödeme (transaction) listesi satırı.
 * buyerEmail/buyerName: PRODUCT_ORDER'da alıcı; tenantName: ilgili mağaza adı (Feign ile çözülür).
 */
public record AdminPaymentSummaryResponse(
        Long id,
        Long orderId,
        Long subscriptionId,
        Long tenantId,
        String tenantName,
        Long customerId,
        String buyerEmail,
        String buyerName,
        String paymentType,
        BigDecimal amount,
        BigDecimal commissionAmount,
        BigDecimal commissionRate,
        BigDecimal netAmount,
        BigDecimal refundedAmount,
        String paymentStatus,
        String paymentMethod,
        String iyzicoTransactionId,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {}
