package com.ecommerce.paymentservice.payment.domain;

/**
 * Yeni kart ekleme komutu. Controller'daki AddCardRequest DTO'sundan map'lenir.
 * Kart verisi yalnızca iyzico'ya iletilmek üzere taşınır; DB'ye hiçbir hassas alan yazılmaz.
 */
public record AddCardCommand(
        String cardAlias,
        String cardHolderName,
        String cardNumber,
        String expireMonth,
        String expireYear
) {}
