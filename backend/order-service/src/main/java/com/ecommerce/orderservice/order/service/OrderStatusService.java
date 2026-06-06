package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.order.command.UpdateOrderStatusCommand;
import com.ecommerce.orderservice.order.entity.Order;

public interface OrderStatusService {
    Order updateStatus(UpdateOrderStatusCommand command);
}
