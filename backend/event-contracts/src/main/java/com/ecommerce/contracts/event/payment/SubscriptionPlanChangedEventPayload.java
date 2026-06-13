package com.ecommerce.contracts.event.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abonelik planı değiştiğinde yayınlanır.
 * changeType:
 *  - UPGRADE             : üst plana anında geçildi, prorate fark (chargedAmount) tahsil edildi.
 *  - DOWNGRADE_SCHEDULED : alt plana geçiş döngü sonunda (effectiveDate) uygulanacak; tahsilat yok.
 */
public record SubscriptionPlanChangedEventPayload(
        Long tenantId,
        String contactEmail,
        String oldPlanName,
        String newPlanName,
        String changeType,
        LocalDate effectiveDate,
        BigDecimal chargedAmount
) {}
