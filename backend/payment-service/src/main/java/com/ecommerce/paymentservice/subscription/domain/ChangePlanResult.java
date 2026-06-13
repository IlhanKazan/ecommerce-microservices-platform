package com.ecommerce.paymentservice.subscription.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Plan değiştirme sonucunu controller'a taşıyan Info record'u.
 * effectiveDate: UPGRADE'de bugün, DOWNGRADE_SCHEDULED'da bir sonraki billing tarihi.
 */
public record ChangePlanResult(
        PlanChangeType changeType,
        BigDecimal chargedAmount,
        LocalDate effectiveDate,
        String newPlanName
) implements Serializable {}
