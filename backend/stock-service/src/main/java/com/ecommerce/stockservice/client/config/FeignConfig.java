package com.ecommerce.stockservice.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"com.ecommerce.stockservice.client", "com.ecommerce.common"})
public class FeignConfig {
}