import { api } from '../../../lib/axios.ts';
import { API_ENDPOINTS } from '../../../config/apiEndpoints.ts';
import { normalizePage } from '../../../utils/pageResponse.ts';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils.ts';
import type { PageResponse, TenantSummary, TenantDetail, TenantStatus } from '../../../types/tenant.ts';
import type {
    AdminCategory, CreateCategoryRequest, UpdateCategoryRequest,
    AdminOrderSummary, AdminOrderDetail, AdminOrderStats, AdminUser,
    AdminPaymentStats, AdminProductStats, AdminTransaction, AdminProduct,
} from '../../../types/admin.ts';

export interface AdminStoreQuery {
    page: number;
    size: number;
    status?: TenantStatus | null;
    verified?: boolean | null;
    q?: string | null;
}

export interface AdminOrderQuery {
    page: number;
    size: number;
    status?: string | null;
    tenantId?: number | null;
}

export interface AdminUserQuery {
    page: number;
    size: number;
    q?: string | null;
    active?: boolean | null;
}

export interface AdminTransactionQuery {
    page: number;
    size: number;
    type?: string | null;
    status?: string | null;
    tenantId?: number | null;
}

export interface AdminProductQueryParams {
    page: number;
    size: number;
    q?: string | null;
    tenantId?: number | null;
    categoryId?: number | null;
    status?: string | null;
}

export const adminService = {
    // ── Stores ──────────────────────────────────────────────────────────
    getStores: async (query: AdminStoreQuery): Promise<PageResponse<TenantSummary>> => {
        const params: Record<string, unknown> = { page: query.page, size: query.size };
        if (query.status) params.status = query.status;
        if (query.verified != null) params.verified = query.verified;
        if (query.q) params.q = query.q;
        const response = await api.get<unknown>(API_ENDPOINTS.ADMIN.STORES, { params });
        return normalizePage<TenantSummary>(response.data);
    },

    getStoreDetail: async (tenantId: number): Promise<TenantDetail> => {
        const response = await api.get<TenantDetail>(API_ENDPOINTS.ADMIN.STORE_BY_ID(tenantId));
        return response.data;
    },

    suspendStore: async (tenantId: number, idempotencyKey: string): Promise<void> => {
        await api.post(API_ENDPOINTS.ADMIN.STORE_SUSPEND(tenantId), undefined, {
            headers: { [IDEMPOTENCY_KEY_HEADER]: idempotencyKey },
        });
    },

    reactivateStore: async (tenantId: number, idempotencyKey: string): Promise<void> => {
        await api.post(API_ENDPOINTS.ADMIN.STORE_REACTIVATE(tenantId), undefined, {
            headers: { [IDEMPOTENCY_KEY_HEADER]: idempotencyKey },
        });
    },

    // ── Categories ──────────────────────────────────────────────────────
    getCategories: async (): Promise<AdminCategory[]> => {
        const response = await api.get<AdminCategory[]>(API_ENDPOINTS.ADMIN.CATEGORIES);
        return response.data;
    },

    createCategory: async (payload: CreateCategoryRequest, idempotencyKey: string): Promise<AdminCategory> => {
        const response = await api.post<AdminCategory>(API_ENDPOINTS.ADMIN.CATEGORIES, payload, {
            headers: { [IDEMPOTENCY_KEY_HEADER]: idempotencyKey },
        });
        return response.data;
    },

    updateCategory: async (id: number, payload: UpdateCategoryRequest, idempotencyKey: string): Promise<AdminCategory> => {
        const response = await api.put<AdminCategory>(API_ENDPOINTS.ADMIN.CATEGORY_BY_ID(id), payload, {
            headers: { [IDEMPOTENCY_KEY_HEADER]: idempotencyKey },
        });
        return response.data;
    },

    setCategoryStatus: async (id: number, active: boolean): Promise<AdminCategory> => {
        const response = await api.patch<AdminCategory>(
            API_ENDPOINTS.ADMIN.CATEGORY_STATUS(id),
            undefined,
            { params: { active } },
        );
        return response.data;
    },

    deleteCategory: async (id: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.ADMIN.CATEGORY_BY_ID(id));
    },

    uploadCategoryImage: async (id: number, file: File): Promise<AdminCategory> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post<AdminCategory>(API_ENDPOINTS.ADMIN.CATEGORY_IMAGE(id), formData, {
            headers: { 'Content-Type': 'multipart/form-data' },
        });
        return response.data;
    },

    // ── Orders ──────────────────────────────────────────────────────────
    getOrders: async (query: AdminOrderQuery): Promise<PageResponse<AdminOrderSummary>> => {
        const params: Record<string, unknown> = { page: query.page, size: query.size };
        if (query.status) params.status = query.status;
        if (query.tenantId) params.tenantId = query.tenantId;
        const response = await api.get<unknown>(API_ENDPOINTS.ADMIN.ORDERS, { params });
        return normalizePage<AdminOrderSummary>(response.data);
    },

    getOrderDetail: async (orderId: number): Promise<AdminOrderDetail> => {
        const response = await api.get<AdminOrderDetail>(API_ENDPOINTS.ADMIN.ORDER_BY_ID(orderId));
        return response.data;
    },

    getOrderStats: async (): Promise<AdminOrderStats> => {
        const response = await api.get<AdminOrderStats>(API_ENDPOINTS.ADMIN.ORDER_STATS);
        return response.data;
    },

    // ── Users ───────────────────────────────────────────────────────────
    getUsers: async (query: AdminUserQuery): Promise<PageResponse<AdminUser>> => {
        const params: Record<string, unknown> = { page: query.page, size: query.size };
        if (query.q) params.q = query.q;
        if (query.active != null) params.active = query.active;
        const response = await api.get<unknown>(API_ENDPOINTS.ADMIN.USERS, { params });
        return normalizePage<AdminUser>(response.data);
    },

    setUserStatus: async (userId: number, active: boolean): Promise<AdminUser> => {
        const response = await api.patch<AdminUser>(
            API_ENDPOINTS.ADMIN.USER_STATUS(userId),
            undefined,
            { params: { active } },
        );
        return response.data;
    },

    // ── Gelir / Overview ────────────────────────────────────────────────
    getPaymentStats: async (): Promise<AdminPaymentStats> => {
        const response = await api.get<AdminPaymentStats>(API_ENDPOINTS.ADMIN.PAYMENT_STATS);
        return response.data;
    },

    getProductStats: async (): Promise<AdminProductStats> => {
        const response = await api.get<AdminProductStats>(API_ENDPOINTS.ADMIN.PRODUCT_STATS);
        return response.data;
    },

    // ── Transactions (ödemeler) ─────────────────────────────────────────
    getTransactions: async (query: AdminTransactionQuery): Promise<PageResponse<AdminTransaction>> => {
        const params: Record<string, unknown> = { page: query.page, size: query.size };
        if (query.type) params.type = query.type;
        if (query.status) params.status = query.status;
        if (query.tenantId) params.tenantId = query.tenantId;
        const response = await api.get<unknown>(API_ENDPOINTS.ADMIN.TRANSACTIONS, { params });
        return normalizePage<AdminTransaction>(response.data);
    },

    // ── Ürünler ─────────────────────────────────────────────────────────
    getProducts: async (query: AdminProductQueryParams): Promise<PageResponse<AdminProduct>> => {
        const params: Record<string, unknown> = { page: query.page, size: query.size };
        if (query.q) params.q = query.q;
        if (query.tenantId) params.tenantId = query.tenantId;
        if (query.categoryId) params.categoryId = query.categoryId;
        if (query.status) params.status = query.status;
        const response = await api.get<unknown>(API_ENDPOINTS.ADMIN.PRODUCTS, { params });
        return normalizePage<AdminProduct>(response.data);
    },

    deactivateProduct: async (id: number): Promise<void> => {
        await api.patch(API_ENDPOINTS.ADMIN.PRODUCT_DEACTIVATE(id));
    },

    removeProduct: async (id: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.ADMIN.PRODUCT_DELETE(id));
    },

    // ── Sistem / Bakım ──────────────────────────────────────────────────
    reindexProducts: async (): Promise<string> => {
        const response = await api.post<string>(API_ENDPOINTS.ADMIN.REINDEX);
        return response.data;
    },

    resyncStocks: async (): Promise<string> => {
        const response = await api.post<string>(API_ENDPOINTS.ADMIN.RESYNC);
        return response.data;
    },

    reconcileReservations: async (): Promise<string> => {
        const response = await api.post<string>(API_ENDPOINTS.ADMIN.RECONCILE);
        return response.data;
    },

    clearCaches: async (): Promise<string> => {
        const [p, t] = await Promise.all([
            api.post<string>(API_ENDPOINTS.ADMIN.CLEAR_CACHE_PRODUCT),
            api.post<string>(API_ENDPOINTS.ADMIN.CLEAR_CACHE_TENANT),
        ]);
        return `${p.data} ${t.data}`;
    },
};
