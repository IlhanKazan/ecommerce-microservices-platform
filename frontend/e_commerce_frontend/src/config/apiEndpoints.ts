export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const API_ENDPOINTS = {
    USER: {
        BASE: '/users',
        ME: '/users/me',
        UPDATE: '/users/update',
        UPLOAD_PROFILE_IMAGE: '/users/upload-profile-image',
        BY_ID: (id: number) => `/users/${id}`,
        ADDRESSES: '/users/addresses',
        ADDRESS_BY_ID: (id: number) => `/users/addresses/${id}`,
        DEFAULT_ADDRESS: (id: number) => `/users/addresses/${id}/default`,
        ACTIVITY_VIEWS: '/users/activity/views',
        ACTIVITY_RECENTLY_VIEWED: '/users/activity/recently-viewed',
        ACTIVITY_SEARCHES: '/users/activity/searches',
        ACTIVITY_RECENT_SEARCHES: '/users/activity/recent-searches',
    },
    TENANT: {
        CREATE: '/tenants',
        ME: '/tenants/me',
        BY_ID: (id: number) => `/tenants/${id}`,
        UPDATE: (id: number) => `/tenants/${id}`,
        UPLOAD_LOGO: (id: number) => `/tenants/${id}/logo`,
        UPDATE_GENERAL: (id: number) => `/tenants/general/${id}`,
        UPDATE_CRITICAL: (id: number) => `/tenants/critical/${id}`,
        ADDRESSES: (tenantId: number) => `/tenants/${tenantId}/addresses`,
        ADDRESS_BY_ID: (tenantId: number, addressId: number) =>
            `/tenants/${tenantId}/addresses/${addressId}`,
        ADD_MEMBER: (tenantId: number) => `/tenants/${tenantId}/members`,
        UPDATE_MEMBER_ROLE: (tenantId: number, memberId: number) =>
            `/tenants/${tenantId}/members/${memberId}`,
        REMOVE_MEMBER: (tenantId: number, memberId: number) =>
            `/tenants/${tenantId}/members/${memberId}`,
        SUBSCRIPTION_DETAIL: (tenantId: number) => `/tenants/${tenantId}/subscription`,
        PAYMENT_HISTORY: (tenantId: number) => `/tenants/${tenantId}/payment-details`,
        RETRY_PAYMENT: (tenantId: number) => `/tenants/${tenantId}/retry-payment`,
        VERIFY_TENANT: (tenantId: number) => `/tenants/${tenantId}/verification`,
        PAUSE: (tenantId: number) => `/tenants/${tenantId}/pause`,
        RESUME: (tenantId: number) => `/tenants/${tenantId}/resume`,
        CLOSE: (tenantId: number) => `/tenants/${tenantId}/close`,
        STOREFRONT: (id: number) => `/public/tenants/${id}/storefront`,
    },
    SUBSCRIPTION: {
        PLANS: '/subscriptions/plans',
        CHANGE_PLAN: '/subscriptions/change-plan',
        CARDS: '/subscriptions/cards',
        CARD_BY_ID: (cardId: number) => `/subscriptions/cards/${cardId}`,
        CARD_DEFAULT: (cardId: number) => `/subscriptions/cards/${cardId}/default`,
    },
    PAYMENT: {},
    SEARCH: {
        PRODUCTS: '/public/search/products',
        PRODUCTS_BY_IDS: '/public/search/products/by-ids',
        AUTOCOMPLETE: '/public/search/autocomplete',
        BRANDS: '/public/search/brands',
        AVAILABILITY: (id: number) => `/public/search/products/${id}/availability`,
        SIMILAR: (productId: number) => `/public/search/recommendations/similar/${productId}`,
        RELATED: '/public/search/recommendations/related',
    },
    PRODUCT: {
        BY_ID_PUBLIC: (id: number) => `/public/products/${id}`,
        FAVORITE: (id: number) => `/public/products/${id}/favorite`,
        FAVORITES: '/public/products/me/favorites',
        FAVORITE_IDS: '/public/products/me/favorite-ids',
        REVIEWS: (id: number) => `/public/products/${id}/reviews`,
        REVIEW_HELPFUL: (productId: number, reviewId: number) =>
            `/public/products/${productId}/reviews/${reviewId}/helpful`,
        REVIEW_DELETE: (productId: number, reviewId: number) =>
            `/public/products/${productId}/reviews/${reviewId}`,
        REVIEW_IMAGE_UPLOAD: (productId: number) =>
            `/public/products/${productId}/reviews/images/upload`,

        // Tenant endpoints
        TENANT_LIST: (tenantId: number) => `/products/tenants/${tenantId}`,
        TENANT_CREATE: (tenantId: number) => `/products/tenants/${tenantId}`,
        TENANT_BY_ID: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}`,
        TENANT_DETAIL: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}/detail`,
        TENANT_UPDATE: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}`,
        TENANT_DELETE: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}`,
        TENANT_SALES_STATUS: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}/sales-status`,
        TENANT_FEATURED: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}/featured`,
        SELLER_RESPONSE: (tenantId: number, productId: number, reviewId: number) =>
            `/products/tenants/${tenantId}/${productId}/reviews/${reviewId}/response`,
        TENANT_IMAGE_UPLOAD: (tenantId: number) =>
            `/products/tenants/${tenantId}/images/upload`,
        // Varyant (child product) yönetimi
        TENANT_VARIANTS: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}/variants`,
        TENANT_VARIANTS_BATCH: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}/variants/batch`,
        TENANT_VARIANT_BY_ID: (tenantId: number, variantId: number) =>
            `/products/tenants/${tenantId}/variants/${variantId}`,
    },
    CATEGORY: {
        ALL: '/categories',
        BY_SLUG: (slug: string) => `/categories/${slug}`,
    },
    ADMIN: {
        STORES: '/tenants/admin/stores',
        STORE_BY_ID: (tenantId: number) => `/tenants/admin/stores/${tenantId}`,
        STORE_SUSPEND: (tenantId: number) => `/tenants/admin/stores/${tenantId}/suspend`,
        STORE_REACTIVATE: (tenantId: number) => `/tenants/admin/stores/${tenantId}/reactivate`,
        CATEGORIES: '/products/admin/categories',
        CATEGORY_BY_ID: (id: number) => `/products/admin/categories/${id}`,
        CATEGORY_STATUS: (id: number) => `/products/admin/categories/${id}/status`,
        CATEGORY_IMAGE: (id: number) => `/products/admin/categories/${id}/image`,
        ORDERS: '/orders/admin/orders',
        ORDER_BY_ID: (orderId: number) => `/orders/admin/orders/${orderId}`,
        ORDER_STATS: '/orders/admin/stats',
        USERS: '/users/admin/users',
        USER_STATUS: (userId: number) => `/users/admin/users/${userId}/status`,
        PAYMENT_STATS: '/payments/admin/stats',
        TRANSACTIONS: '/payments/admin/payments',
        PRODUCT_STATS: '/products/admin/stats',
        PRODUCTS: '/products/admin/products',
        PRODUCT_DEACTIVATE: (id: number) => `/products/admin/products/${id}/deactivate`,
        PRODUCT_DELETE: (id: number) => `/products/admin/products/${id}`,
        // Sistem / Bakım operasyonları
        REINDEX: '/products/admin/reindex',
        RESYNC: '/stocks/admin/resync',
        RECONCILE: '/stocks/admin/reconcile-reservations',
        CLEAR_CACHE_PRODUCT: '/products/admin/clear-cache',
        CLEAR_CACHE_TENANT: '/tenants/admin/clear-cache',
    },
    BASKET: {
        GET:         '/baskets/me',
        ADD:         '/baskets/me/items',
        MERGE:       '/baskets/me/merge',
        REMOVE_ITEM: (productId: number) => `/baskets/me/items/${productId}`,
        UPDATE_ITEM: (productId: number) => `/baskets/me/items/${productId}`,
        CLEAR:       '/baskets/me'
    },
    ORDER: {
        CHECKOUT:        (tenantId: number) => `/orders/tenants/${tenantId}`,
        FREQUENTLY_BOUGHT_WITH: (productId: number) => `/public/orders/frequently-bought-with/${productId}`,
        MY_ORDERS:       '/orders/me',
        MY_ORDER_DETAIL: (orderId: number) => `/orders/me/${orderId}`,
        CANCEL:          (orderId: number) => `/orders/me/${orderId}/cancel`,
        RETURN:          (orderId: number) => `/orders/me/${orderId}/return`,
        TENANT_RETURNS:        (tenantId: number) => `/orders/tenants/${tenantId}/returns`,
        TENANT_RETURN_APPROVE: (tenantId: number, orderId: number) =>
            `/orders/tenants/${tenantId}/${orderId}/return/approve`,
        TENANT_RETURN_REJECT:  (tenantId: number, orderId: number) =>
            `/orders/tenants/${tenantId}/${orderId}/return/reject`,
        ADMIN_RETURNS:         '/orders/admin/returns',
        ADMIN_RETURN_APPROVE:  (orderId: number) => `/orders/admin/${orderId}/return/approve`,
        ADMIN_RETURN_REJECT:   (orderId: number) => `/orders/admin/${orderId}/return/reject`,
        TENANT_ORDERS:   (tenantId: number) => `/orders/tenants/${tenantId}`,
        UPDATE_STATUS:   (tenantId: number, orderId: number) =>
            `/orders/tenants/${tenantId}/${orderId}/status`,
        TENANT_ANALYTICS: (tenantId: number) => `/orders/tenants/${tenantId}/analytics`,
        TENANT_PRODUCT_METRICS: (tenantId: number, productId: number) =>
            `/orders/tenants/${tenantId}/products/${productId}/metrics`,
        ADMIN_PRODUCT_METRICS: (productId: number) =>
            `/orders/admin/products/${productId}/metrics`,
    },
    STOCK: {
        WAREHOUSES: (tenantId: number) => `/stocks/tenant/${tenantId}/warehouses`,
        WAREHOUSE_BY_ID: (tenantId: number, warehouseId: number) =>
            `/stocks/tenant/${tenantId}/warehouses/${warehouseId}`,
        WAREHOUSE_STATUS: (tenantId: number, warehouseId: number) =>
            `/stocks/tenant/${tenantId}/warehouses/${warehouseId}/status`,
        MANUAL_ADD: (tenantId: number) => `/stocks/tenant/${tenantId}/manual-add`,
        MANUAL_ADD_BATCH: (tenantId: number) => `/stocks/tenant/${tenantId}/manual-add/batch`,
        MANUAL_REMOVE: (tenantId: number) => `/stocks/tenant/${tenantId}/manual-remove`,
        LOW_STOCK_THRESHOLD: (tenantId: number) => `/stocks/tenant/${tenantId}/low-stock-threshold`,
        SUMMARY: (tenantId: number) => `/stocks/tenant/${tenantId}`,
        // Public — detay sayfası varyant stok durumu (anonim)
        AVAILABILITY: '/public/stocks/availability',
    },
} as const;