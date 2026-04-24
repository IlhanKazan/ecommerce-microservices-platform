package com.ecommerce.productservice.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"com.ecommerce.productservice", "com.ecommerce.common"})
public class FeignConfig {
}
