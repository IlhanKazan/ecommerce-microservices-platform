package com.ecommerce.common.event.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EventConstants {

    public static final String AGGREGATE_PRODUCT = "PRODUCT";
    public static final String AGGREGATE_ORDER = "ORDER";
    public static final String AGGREGATE_INVENTORY = "INVENTORY";

    public static final String EVENT_PRODUCT_CREATED = "PRODUCT_CREATED_EVENT";
    public static final String EVENT_PRODUCT_UPDATED = "PRODUCT_UPDATED_EVENT";
    public static final String EVENT_PRODUCT_DELETED = "PRODUCT_DELETED_EVENT";

    public static final String EVENT_INVENTORY_RESERVED = "INVENTORY_RESERVED_EVENT";
    public static final String EVENT_INVENTORY_FAILED = "INVENTORY_FAILED_EVENT";

}
