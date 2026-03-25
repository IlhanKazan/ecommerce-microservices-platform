package com.ecommerce.stockservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class ApiPaths {
    private ApiPaths() {}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Stocks {
        public static final String STOCKS_PATH = BASE_PATH_V1 + "/stocks";
        public static final String TENANT_STOCKS_PATH = "/tenant/{tenantId}";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Warehouses {
        public static final String WAREHOUSES_PATH = Stocks.STOCKS_PATH + "/tenant/{tenantId}/warehouses";
    }

}
