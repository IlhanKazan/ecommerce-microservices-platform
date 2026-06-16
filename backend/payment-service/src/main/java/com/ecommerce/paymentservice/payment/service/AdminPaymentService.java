package com.ecommerce.paymentservice.payment.service;

import com.ecommerce.paymentservice.payment.constant.PaymentStatus;
import com.ecommerce.paymentservice.payment.constant.PaymentType;
import com.ecommerce.paymentservice.payment.controller.dto.response.AdminPaymentStatsResponse;
import com.ecommerce.paymentservice.payment.controller.dto.response.AdminPaymentStatsResponse.MonthlyRevenue;
import com.ecommerce.paymentservice.payment.controller.dto.response.AdminPaymentSummaryResponse;
import com.ecommerce.paymentservice.client.UserTenantAuthzClient;
import com.ecommerce.paymentservice.payment.entity.Payment;
import com.ecommerce.paymentservice.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Platform admin ödeme istatistikleri + transaction listesi.
 * Yetki controller'da (@PreAuthorize hasRole platform-admin).
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final UserTenantAuthzClient userTenantClient;

    public AdminPaymentStatsResponse getStats() {
        BigDecimal commissionRevenue = BigDecimal.ZERO;
        BigDecimal subscriptionRevenue = BigDecimal.ZERO;
        long productPaymentCount = 0;
        long subscriptionPaymentCount = 0;

        for (Object[] row : paymentRepository.revenueByType(PaymentStatus.SUCCESS)) {
            PaymentType type = (PaymentType) row[0];
            BigDecimal amount = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal commission = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            long count = (Long) row[3];
            if (type == PaymentType.PRODUCT_ORDER) {
                commissionRevenue = commission;
                productPaymentCount = count;
            } else if (type == PaymentType.SUBSCRIPTION) {
                subscriptionRevenue = amount;
                subscriptionPaymentCount = count;
            }
        }

        BigDecimal totalRevenue = commissionRevenue.add(subscriptionRevenue);

        List<MonthlyRevenue> monthly = paymentRepository.monthlyRevenue(PaymentStatus.SUCCESS).stream()
                .map(row -> new MonthlyRevenue(
                        (String) row[0],
                        row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO,
                        row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO))
                .toList();

        return new AdminPaymentStatsResponse(
                commissionRevenue, subscriptionRevenue, totalRevenue,
                productPaymentCount, subscriptionPaymentCount, monthly);
    }

    public Page<AdminPaymentSummaryResponse> listPayments(PaymentType type, PaymentStatus status,
                                                          Long tenantId, Pageable pageable) {
        Page<Payment> page = paymentRepository.searchForAdmin(type, status, tenantId, pageable);

        // Sayfadaki distinct tenantId'ler için tek Feign çağrısıyla isim çöz (N+1 yok).
        List<Long> tenantIds = page.getContent().stream()
                .map(Payment::getTenantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, String> tenantNames = resolveTenantNames(tenantIds);

        return page.map(p -> toSummary(p, tenantNames));
    }

    private Map<Long, String> resolveTenantNames(List<Long> tenantIds) {
        if (tenantIds.isEmpty()) {
            return Map.of();
        }
        try {
            return userTenantClient.getTenantNames(tenantIds);
        } catch (Exception e) {
            log.warn("Tenant adları çözülemedi (transaction listesi), id'ler gösterilecek: {}", e.getMessage());
            return Map.of();
        }
    }

    private AdminPaymentSummaryResponse toSummary(Payment p, Map<Long, String> tenantNames) {
        return new AdminPaymentSummaryResponse(
                p.getId(),
                p.getOrderId(),
                p.getSubscriptionId(),
                p.getTenantId(),
                p.getTenantId() != null ? tenantNames.get(p.getTenantId()) : null,
                p.getCustomerId(),
                p.getBuyerEmail(),
                p.getBuyerName(),
                p.getPaymentType() != null ? p.getPaymentType().name() : null,
                p.getAmount(),
                p.getCommissionAmount(),
                p.getCommissionRate(),
                p.getNetAmount(),
                p.getRefundedAmount(),
                p.getPaymentStatus() != null ? p.getPaymentStatus().name() : null,
                p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null,
                p.getIyzicoTransactionId(),
                p.getPaidAt(),
                p.getCreatedAt());
    }
}
