package com.ecommerce.paymentservice.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {"com.ecommerce.paymentservice", "com.ecommerce.common"})
public class FeignConfig {
}
