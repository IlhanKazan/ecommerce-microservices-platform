package com.ecommerce.orderservice.order.service;

import com.ecommerce.orderservice.order.entity.Order;
import com.ecommerce.orderservice.order.query.ReturnInfo;

import java.util.List;
import java.util.UUID;

public interface OrderReturnService {

    // Müşteri teslim edilmiş sipariş için iade talebi açar (14 gün penceresi + yapılandırılmış sebep)
    Order requestReturn(Long orderId, UUID userId, String reasonCode, String note);

    // Onay: gerçek iyzico Refund + RETURNED + restock event. tenantId null → admin override.
    Order approveReturn(Long orderId, Long tenantId, String note);

    // Red: DELIVERED'a döner. tenantId null → admin override.
    Order rejectReturn(Long orderId, Long tenantId, String note);

    List<ReturnInfo> getTenantReturns(Long tenantId);

    List<ReturnInfo> getAllReturns();
}
