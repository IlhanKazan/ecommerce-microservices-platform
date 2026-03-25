package com.ecommerce.basketservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EntityScan(basePackages = "com.ecommerce.basketservice")
@EnableFeignClients(basePackages = {"com.ecommerce.basketservice", "com.ecommerce.common"})
@ComponentScan(basePackages = {"com.ecommerce.basketservice", "com.ecommerce.common"})
public class BasketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasketServiceApplication.class, args);
    }

}
