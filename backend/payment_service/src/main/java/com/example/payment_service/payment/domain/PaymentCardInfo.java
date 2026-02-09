package com.example.payment_service.payment.domain;

public record PaymentCardInfo(
        String holderName,
        String number,
        String expireMonth,
        String expireYear,
        String cvc
) {
}
