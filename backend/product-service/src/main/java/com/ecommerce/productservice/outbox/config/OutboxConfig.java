package com.ecommerce.productservice.outbox.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @Configuration olmadan @EnableScheduling devreye girmiyordu (bean değildi) → scheduler'lar çalışmıyordu.
@Configuration
@EnableScheduling
public class OutboxConfig {
}
