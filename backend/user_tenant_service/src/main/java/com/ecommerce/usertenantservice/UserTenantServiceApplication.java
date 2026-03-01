package com.ecommerce.usertenantservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan(basePackages = "com.ecommerce.usertenantservice")
@EnableFeignClients(basePackages = {"com.ecommerce.usertenantservice", "com.ecommerce.common"})
@ComponentScan(basePackages = {"com.ecommerce.usertenantservice", "com.ecommerce.common"})
public class UserTenantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserTenantServiceApplication.class, args);
    }
}
