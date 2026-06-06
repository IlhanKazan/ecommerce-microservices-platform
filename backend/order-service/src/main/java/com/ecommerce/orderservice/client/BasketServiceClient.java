package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.BasketResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "basket-service", url = "${application.clients.basket.url}")
public interface BasketServiceClient {

    @GetMapping("/api/v1/baskets/me")
    BasketResponse getMyBasket();

    @DeleteMapping("/api/v1/baskets/me")
    void clearMyBasket();
}
