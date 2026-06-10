package com.ecommerce.paymentservice.payment.constant;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILURE,
    REFUNDED,
    // iyzico ödeme başarılı (para çekildi) ama abonelik provisioning patladı — para izi bu state'le kalıcı kayıt altına alınır
    PROVISION_FAILED
}
