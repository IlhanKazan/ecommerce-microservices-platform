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
    },
    SUBSCRIPTION: {
        PLANS: '/subscriptions/plans',
        CHANGE_PLAN: '/subscriptions/change-plan',
    },
    PAYMENT: {},
    SEARCH: {
        PRODUCTS: '/public/search/products',
    },
    PRODUCT: {
        BY_ID_PUBLIC: (id: number) => `/public/products/${id}`,
        REVIEWS: (id: number) => `/public/products/${id}/reviews`,
        REVIEW_HELPFUL: (productId: number, reviewId: number) =>
            `/public/products/${productId}/reviews/${reviewId}/helpful`,
        REVIEW_DELETE: (productId: number, reviewId: number) =>
            `/public/products/${productId}/reviews/${reviewId}`,

        // Tenant endpoints
        TENANT_LIST: (tenantId: number) => `/products/tenants/${tenantId}`,
        TENANT_CREATE: (tenantId: number) => `/products/tenants/${tenantId}`,
        TENANT_BY_ID: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}`,
        TENANT_UPDATE: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}`,
        TENANT_DELETE: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}`,
        TENANT_SALES_STATUS: (tenantId: number, productId: number) =>
            `/products/tenants/${tenantId}/${productId}/sales-status`,
        SELLER_RESPONSE: (tenantId: number, productId: number, reviewId: number) =>
            `/products/tenants/${tenantId}/${productId}/reviews/${reviewId}/response`,
    },
    CATEGORY: {
        ALL: '/categories',
        BY_SLUG: (slug: string) => `/categories/${slug}`,
    },
    BASKET: {
        GET:         '/baskets/me',
        ADD:         '/baskets/me/items',
        REMOVE_ITEM: (productId: number) => `/baskets/me/items/${productId}`,
        CLEAR:       '/baskets/me'
    },
    ORDER: {
        CREATE: '/orders',
    },
    STOCK: {
        WAREHOUSES: (tenantId: number) => `/stocks/tenant/${tenantId}/warehouses`,
        MANUAL_ADD: (tenantId: number) => `/stocks/tenant/${tenantId}/manual-add`,
    },
} as const;