package com.ecommerce.usertenantservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

public class ApiPaths {
    private ApiPaths() {}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class User{
        public static final String USER = BASE_PATH_V1 + "/users";
        public static final String ADDRESS = BASE_PATH_V1 + "/users/addresses";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Tenant{
        public static final String TENANT = BASE_PATH_V1 + "/tenants";
        public static final String PUBLIC_TENANT = BASE_PATH_V1 + "/public/tenants";
    }

}
