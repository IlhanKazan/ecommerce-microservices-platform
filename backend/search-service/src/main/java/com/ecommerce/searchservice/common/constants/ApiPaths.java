package com.ecommerce.searchservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class ApiPaths {
    private ApiPaths(){}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PublicProduct {
        public static final String PUBLIC_SEARCH_PRODUCTS = BASE_PATH_V1 + "/public/search/products";
    }
}
