package com.ecommerce.spi.config;

public class SpiConfig {

    private SpiConfig() {}

    public static final String KEYCLOAK_TOKEN_URL = System.getenv("MY_SPI_KEYCLOAK_TOKEN_URL");
    public static final String CLIENT_ID = System.getenv("MY_SPI_CLIENT_ID");
    public static final String CLIENT_SECRET = System.getenv("MY_SPI_CLIENT_SECRET");

    public static final String USER_SERVICE_BASE_URL = System.getenv("MY_SPI_USER_SERVICE_BASE_URL");
    public static final String USER_SERVICE_HEALTH_URL = System.getenv("MY_SPI_USER_SERVICE_HEALTH_URL");

}
