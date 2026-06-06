package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.client.dto.BasketResponse;
import com.ecommerce.orderservice.client.dto.PaymentResult;
import com.ecommerce.orderservice.client.dto.ProductSnapshotInfo;
import com.ecommerce.orderservice.order.command.CheckoutCommand;
import com.ecommerce.orderservice.order.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public interface OrderPersistenceService {
    Order saveOrderWithItems(
            CheckoutCommand command,
            BasketResponse basket,
            List<ProductSnapshotInfo> snapshots,
            BigDecimal totalAmount,
            String currency,
            PaymentResult paymentResult
    );
}
