package com.ecommerce.paymentservice.payment.domain;

/**
 * iyzico kart saklama (Card Storage) işleminin sonucu.
 * Kart numarası/CVC içermez; sadece kayıtlı kartı temsil eden token + cüzdan anahtarı
 * ve maskeli görsel bilgiler (son 4 hane, marka) taşınır.
 */
public record IyzicoStoredCard(
        String cardToken,
        String cardUserKey,
        String lastFour,
        String cardAssociation,
        String cardFamily,
        String binNumber
) {}
