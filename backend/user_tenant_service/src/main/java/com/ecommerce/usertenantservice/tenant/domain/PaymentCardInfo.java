package com.ecommerce.usertenantservice.tenant.domain;

public record PaymentCardInfo(
        String holderName,
        String number,
        String expireMonth,
        String expireYear,
        String cvc
) {
}
