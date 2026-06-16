package com.ecommerce.orderservice.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiPaths {

    public static final String ORDER_BASE          = "/api/v1/orders";
    public static final String MY_ORDERS           = ORDER_BASE + "/me";
    // Public (anonim): co-purchase katalog verisi — GlobalSecurityConfig /api/v1/public/** permitAll + gateway public/orders route
    public static final String FREQUENTLY_BOUGHT_WITH = "/api/v1/public/orders/frequently-bought-with/{productId}";
    public static final String MY_ORDER_DETAIL     = MY_ORDERS + "/{orderId}";
    public static final String MY_ORDER_CANCEL     = MY_ORDERS + "/{orderId}/cancel";
    public static final String MY_ORDER_RETURN     = MY_ORDERS + "/{orderId}/return";
    public static final String TENANT_ORDERS       = ORDER_BASE + "/tenants/{tenantId}";
    public static final String TENANT_ORDER_STATUS = TENANT_ORDERS + "/{orderId}/status";
    public static final String TENANT_ANALYTICS    = TENANT_ORDERS + "/analytics";
    public static final String TENANT_PRODUCT_METRICS = TENANT_ORDERS + "/products/{productId}/metrics";
    public static final String TENANT_RETURNS         = TENANT_ORDERS + "/returns";
    public static final String TENANT_RETURN_APPROVE  = TENANT_ORDERS + "/{orderId}/return/approve";
    public static final String TENANT_RETURN_REJECT   = TENANT_ORDERS + "/{orderId}/return/reject";

    // Platform admin — /api/v1/orders/** gateway route'u altında, method-level hasRole ile korunur
    public static final String ADMIN_ORDERS         = ORDER_BASE + "/admin/orders";
    public static final String ADMIN_ORDER_DETAIL   = ADMIN_ORDERS + "/{orderId}";
    public static final String ADMIN_ORDER_STATS    = ORDER_BASE + "/admin/stats";
    public static final String ADMIN_PRODUCT_METRICS = ORDER_BASE + "/admin/products/{productId}/metrics";
    public static final String ADMIN_RETURNS         = ORDER_BASE + "/admin/returns";
    public static final String ADMIN_RETURN_APPROVE  = ORDER_BASE + "/admin/{orderId}/return/approve";
    public static final String ADMIN_RETURN_REJECT   = ORDER_BASE + "/admin/{orderId}/return/reject";
}
