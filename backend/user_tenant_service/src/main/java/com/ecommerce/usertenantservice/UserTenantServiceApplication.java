package com.ecommerce.usertenantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EntityScan(basePackages = "com.ecommerce.usertenantservice")
@EnableFeignClients
public class UserTenantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserTenantServiceApplication.class, args);
    }
}
