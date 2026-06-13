package com.ecommerce.paymentservice.subscription.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Plan değiştirme sonucu — frontend onay/bilgi mesajı için")
public record ChangePlanResponse(

        @Schema(description = "UPGRADE | DOWNGRADE_SCHEDULED | DOWNGRADE_CANCELED")
        String changeType,

        @Schema(description = "Upgrade'de anında çekilen prorate fark; diğer durumlarda 0")
        BigDecimal chargedAmount,

        @Schema(description = "Değişikliğin geçerlilik tarihi (upgrade: bugün, downgrade: sonraki billing)")
        LocalDate effectiveDate,

        String newPlanName
) {}
