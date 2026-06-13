package com.ecommerce.paymentservice.subscription.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Abonelik planı değiştirme isteği")
public record ChangePlanRequest(

        @Schema(description = "Aboneliği değişecek tenant", example = "12")
        Long tenantId,

        @Schema(description = "Geçilecek yeni planın id'si", example = "3")
        Long planId
) {}
