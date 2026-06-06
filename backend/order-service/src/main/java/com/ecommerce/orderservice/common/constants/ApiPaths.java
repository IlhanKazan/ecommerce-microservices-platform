package com.ecommerce.orderservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

    public static final String ORDER_BASE          = "/api/v1/orders";
    public static final String MY_ORDERS           = ORDER_BASE + "/me";
    public static final String MY_ORDER_DETAIL     = MY_ORDERS + "/{orderId}";
    public static final String MY_ORDER_CANCEL     = MY_ORDERS + "/{orderId}/cancel";
    public static final String TENANT_ORDERS       = ORDER_BASE + "/tenants/{tenantId}";
    public static final String TENANT_ORDER_STATUS = TENANT_ORDERS + "/{orderId}/status";
}
