package com.ecommerce.orderservice.order.constant;

public enum ReturnStatus {
    REQUESTED,  // Müşteri iade talebi açtı, bekliyor
    APPROVED,   // Merchant/admin onayladı, para iadesi yapıldı
    REJECTED    // Merchant/admin reddetti
}
