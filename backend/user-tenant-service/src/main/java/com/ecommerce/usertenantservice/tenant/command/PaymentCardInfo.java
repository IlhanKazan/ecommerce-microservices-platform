package com.ecommerce.usertenantservice.tenant.command;

public record PaymentCardInfo(
        String holderName,
        String number,
        String expireMonth,
        String expireYear,
        String cvc
) {
}
