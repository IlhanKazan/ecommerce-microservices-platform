package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.order.command.CheckoutCommand;
import com.ecommerce.orderservice.order.entity.Order;

public interface OrderSagaService {
    Order checkout(CheckoutCommand command);
}
