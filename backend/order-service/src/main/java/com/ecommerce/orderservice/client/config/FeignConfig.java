package com.ecommerce.orderservice.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"com.ecommerce.orderservice", "com.ecommerce.common"})
public class FeignConfig {
}
