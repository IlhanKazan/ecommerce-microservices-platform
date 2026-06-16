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
    public static final String EVENT_PRODUCT_STATS_CHANGED = "PRODUCT_STATS_CHANGED_EVENT";

    public static final String EVENT_STOCK_RESERVED = "STOCK_RESERVED_EVENT";
    public static final String EVENT_STOCK_FAILED = "STOCK_FAILED_EVENT";
    public static final String EVENT_STOCK_STATUS_CHANGED = "STOCK_STATUS_CHANGED_EVENT";

    public static final String EVENT_PAYMENT_SUCCESS = "PAYMENT_SUCCESS_EVENT";
    public static final String EVENT_PAYMENT_FAILED = "PAYMENT_FAILED_EVENT";
    public static final String EVENT_SUBSCRIPTION_ACTIVATED = "SUBSCRIPTION_ACTIVATED_EVENT";

    public static final String EVENT_TENANT_CREATED = "TENANT_CREATED_EVENT";
    public static final String EVENT_TENANT_ACTIVATED = "TENANT_ACTIVATED_EVENT";
    public static final String EVENT_TENANT_PAYMENT_FAILED = "TENANT_PAYMENT_FAILED_EVENT";
    public static final String EVENT_TENANT_STATUS_CHANGED = "TENANT_STATUS_CHANGED_EVENT";

    public static final String EVENT_ORDER_CREATED          = "ORDER_CREATED_EVENT";
    public static final String EVENT_ORDER_CONFIRMED        = "ORDER_CONFIRMED_EVENT";
    public static final String EVENT_ORDER_CANCELLED        = "ORDER_CANCELLED_EVENT";
    public static final String EVENT_ORDER_SHIPPED          = "ORDER_SHIPPED_EVENT";
    public static final String EVENT_ORDER_REFUNDED         = "ORDER_REFUNDED_EVENT";
    public static final String EVENT_ORDER_DELIVERED        = "ORDER_DELIVERED_EVENT";
    public static final String EVENT_ORDER_RETURN_REQUESTED = "ORDER_RETURN_REQUESTED_EVENT";
    public static final String EVENT_ORDER_RETURN_REJECTED  = "ORDER_RETURN_REJECTED_EVENT";
    public static final String EVENT_ORDER_RETURNED         = "ORDER_RETURNED_EVENT";

    public static final String EVENT_STOCK_COMMIT_FAILED    = "STOCK_COMMIT_FAILED_EVENT";

    public static final String EVENT_SUBSCRIPTION_RENEWAL_SUCCESS = "SUBSCRIPTION_RENEWAL_SUCCESS_EVENT";
    public static final String EVENT_SUBSCRIPTION_RENEWAL_FAILED  = "SUBSCRIPTION_RENEWAL_FAILED_EVENT";
    public static final String EVENT_SUBSCRIPTION_PLAN_CHANGED    = "SUBSCRIPTION_PLAN_CHANGED_EVENT";

    public static final String AGGREGATE_REVIEW = "REVIEW";
    public static final String EVENT_REVIEW_CREATED = "REVIEW_CREATED_EVENT";

}
