package com.ecommerce.paymentservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class ApiPaths {
    private ApiPaths() {}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Payment{
        public static final String PAYMENT = BASE_PATH_V1 + "/payments";
        public static final String PAYMENT_HISTORY = PAYMENT + "/history";
        // Platform admin — /api/v1/payments/** gateway route'u altında, method-level hasRole ile korunur
        public static final String ADMIN = PAYMENT + "/admin";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Subscription{
        public static final String SUBSCRIPTION = BASE_PATH_V1 + "/subscriptions";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Card{
        // api-gateway yalnızca /api/v1/subscriptions/** path'ini payment-service'e yönlendirdiği için
        // kart endpoint'leri de bu prefix altında tutulur (gateway config değişmeden).
        public static final String CARDS = BASE_PATH_V1 + "/subscriptions/cards";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SubMerchant{
        public static final String SUBMERCHANT = BASE_PATH_V1 + "/submerchant";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Internal {
        public static final String INTERNAL_PAYMENT = BASE_PATH_V1 + "/payments/internal";
    }

}

