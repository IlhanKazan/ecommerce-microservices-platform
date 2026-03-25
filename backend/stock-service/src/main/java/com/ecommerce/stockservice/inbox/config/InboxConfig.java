package com.ecommerce.stockservice.inbox.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(value = "app.scheduling.enabled", matchIfMissing = true)
public class InboxConfig {

}