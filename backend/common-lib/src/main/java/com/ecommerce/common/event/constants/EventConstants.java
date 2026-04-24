package com.ecommerce.common.event.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventConstants {

    public static final String AGGREGATE_PRODUCT = "PRODUCT";
    public static final String AGGREGATE_ORDER = "ORDER";
    public static final String AGGREGATE_STOCK = "STOCK";
    public static final String AGGREGATE_PAYMENT = "PAYMENT";
    public static final String AGGREGATE_TENANT = "TENANT";

    public static final String EVENT_PRODUCT_CREATED = "PRODUCT_CREATED_EVENT";
    public static final String EVENT_PRODUCT_UPDATED = "PRODUCT_UPDATED_EVENT";
    public static final String EVENT_PRODUCT_DELETED = "PRODUCT_DELETED_EVENT";

    public static final String EVENT_STOCK_RESERVED = "STOCK_RESERVED_EVENT";
    public static final String EVENT_STOCK_FAILED = "STOCK_FAILED_EVENT";
    public static final String EVENT_STOCK_STATUS_CHANGED = "STOCK_STATUS_CHANGED_EVENT";

    public static final String EVENT_PAYMENT_SUCCESS = "PAYMENT_SUCCESS_EVENT";
    public static final String EVENT_PAYMENT_FAILED = "PAYMENT_FAILED_EVENT";
    public static final String EVENT_SUBSCRIPTION_ACTIVATED = "SUBSCRIPTION_ACTIVATED_EVENT";

    public static final String EVENT_TENANT_CREATED = "TENANT_CREATED_EVENT";
    public static final String EVENT_TENANT_ACTIVATED = "TENANT_ACTIVATED_EVENT";
    public static final String EVENT_TENANT_PAYMENT_FAILED = "TENANT_PAYMENT_FAILED_EVENT";

}
