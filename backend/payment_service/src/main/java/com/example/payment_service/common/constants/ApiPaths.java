package com.example.payment_service.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class ApiPaths {
    private ApiPaths() {}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Payment{
        public static final String PAYMENT = BASE_PATH_V1 + "/payments";
        public static final String PAYMENT_HISTORY = PAYMENT + "/history";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Subscription{
        public static final String SUBSCRIPTION = BASE_PATH_V1 + "/subscriptions";
    }

}

