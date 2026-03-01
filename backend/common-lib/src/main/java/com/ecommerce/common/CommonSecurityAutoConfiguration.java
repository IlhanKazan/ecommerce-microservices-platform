package com.ecommerce.common;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.ecommerce.common.security")
public class CommonSecurityAutoConfiguration {
}