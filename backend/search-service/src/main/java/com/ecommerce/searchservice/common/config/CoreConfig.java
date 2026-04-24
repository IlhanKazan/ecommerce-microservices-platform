package com.ecommerce.searchservice.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.ecommerce.common"})
@EntityScan(basePackages = {"com.ecommerce.searchservice", "com.ecommerce.common"})
public class CoreConfig {
}
