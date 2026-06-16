package com.ecommerce.paymentservice.payment.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Platform admin gelir özeti. Platform geliri =
 * PRODUCT_ORDER komisyonu (commissionAmount) + SUBSCRIPTION ücreti (amount).
 * Yalnız SUCCESS ödemeler sayılır.
 */
public record AdminPaymentStatsResponse(
        BigDecimal commissionRevenue,
        BigDecimal subscriptionRevenue,
        BigDecimal totalRevenue,
        long productPaymentCount,
        long subscriptionPaymentCount,
        List<MonthlyRevenue> monthly
) {
    public record MonthlyRevenue(
            String month,
            BigDecimal subscription,
            BigDecimal commission
    ) {}
}
