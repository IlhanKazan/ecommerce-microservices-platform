package com.ecommerce.usertenantservice.tenant.command;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentCardInfo(
        String holderName,
        @Schema(description = "16-digit card number", example = "5528790000000008")
        String number,
        String expireMonth,
        String expireYear,
        String cvc
) {
}
