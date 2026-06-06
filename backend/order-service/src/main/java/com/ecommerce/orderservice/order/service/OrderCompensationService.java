package com.ecommerce.orderservice.order.service;

public interface OrderCompensationService {

    void handleStockCommitFailed(Long orderId, String transactionId, String reason);
}
