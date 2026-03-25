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
        ME: "/tenants/me",

        BY_ID: (id: number) => `/tenants/${id}`,
        UPDATE: (id: number) => `/tenants/${id}`,
        UPLOAD_LOGO: (id: number) => `/tenants/${id}/logo`,

        UPDATE_GENERAL: (id: number) => `/tenants/general/${id}`,
        UPDATE_CRITICAL: (id: number) => `/tenants/critical/${id}`,

        ADDRESSES: (tenantId: number) => `/tenants/${tenantId}/addresses`,
        ADDRESS_BY_ID: (tenantId: number, addressId: number) => `/tenants/${tenantId}/addresses/${addressId}`,

        ADD_MEMBER: (tenantId: number) =>`/tenants/${tenantId}/members`,
        UPDATE_MEMBER_ROLE: (tenantId: number, memberId: number) => `/tenants/${tenantId}/members/${memberId}`,
        REMOVE_MEMBER: (tenantId: number, memberId: number) => `/tenants/${tenantId}/members/${memberId}`,

        SUBSCRIPTION_DETAIL: (tenantId: number) => `/tenants/${tenantId}/subscription`,
        PAYMENT_HISTORY: (tenantId: number) => `/tenants/${tenantId}/payment-details`,

        RETRY_PAYMENT: (tenantId: number) => `/tenants/${tenantId}/retry-payment`,

        VERIFY_TENANT: (tenantId: number) => `/tenants/${tenantId}/verification`,
    },
    SUBSCRIPTION: {
        PLANS: '/subscriptions/plans',
        CHANGE_PLAN: '/subscriptions/change-plan'
    },
    PAYMENT: {

    },
    ORDER: {
        BASE: '/orders',
        CREATE: '/orders/create',
    },
    SEARCH: {
        PUBLIC: '/public/search/products',
    },
    PRODUCT: {
        BASE: '/products',
        BY_ID_PUBLIC: (id: number) => `/public/products/${id}`,
    },
    STOCK: {
        WAREHOUSES: (tenantId: number) => `/stocks/tenant/${tenantId}/warehouses`,
        MANUAL_ADD: (tenantId: number) => `/stocks/tenant/${tenantId}/manual-add`,
    },
    BASKET: {
        ADD: '/basket/add',
        UPDATE: '/basket/update',
        REMOVE: (productId: number) => `/basket/remove/${productId}`,
    }
} as const;