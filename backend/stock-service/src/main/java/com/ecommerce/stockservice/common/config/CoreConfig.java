package com.ecommerce.stockservice.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.ecommerce.common"})
@EntityScan(basePackages = {"com.ecommerce.stockservice", "com.ecommerce.common"})
@EnableCaching
public class CoreConfig {
}