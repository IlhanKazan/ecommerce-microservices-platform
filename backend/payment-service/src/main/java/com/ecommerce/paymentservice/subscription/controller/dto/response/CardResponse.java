package com.ecommerce.paymentservice.subscription.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kayıtlı kart — maskeli görsel bilgiler. Hassas kart verisi içermez.")
public record CardResponse(
        Long id,
        String cardAlias,
        String lastFour,
        String cardAssociation,
        String cardFamily,
        boolean isDefault
) {}
