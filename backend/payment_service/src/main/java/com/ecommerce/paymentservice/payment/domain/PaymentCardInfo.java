package com.ecommerce.paymentservice.payment.domain;

public record PaymentCardInfo(
        String holderName,
        String number,
        String expireMonth,
        String expireYear,
        String cvc
) {
}
