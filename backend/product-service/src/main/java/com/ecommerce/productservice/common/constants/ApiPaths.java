package com.ecommerce.productservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class ApiPaths {
    private ApiPaths() {}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TenantProduct {
        public static final String TENANT_PRODUCTS = BASE_PATH_V1 + "/products/tenants/{tenantId}";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PublicProduct {
        public static final String PUBLIC_PRODUCTS = BASE_PATH_V1 + "/public/products";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Category {
        public static final String CATEGORIES = BASE_PATH_V1 + "/categories";
    }
}