import { api } from '../../../lib/axios';
import { API_ENDPOINTS } from '../../../config/apiEndpoints';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';
import type { CheckoutRequest, OrderResponse, OrderDetail, OrderPageResponse, MerchantAnalytics, ProductSalesMetrics, OrderReturn } from '../../../types/order';

function idempotencyHeader(key: string) {
    return { [IDEMPOTENCY_KEY_HEADER]: key };
}

export const orderService = {
    createOrder: async (
        tenantId: number,
        payload: CheckoutRequest,
        idempotencyKey: string,
    ): Promise<OrderResponse> => {
        const r = await api.post<OrderResponse>(
            API_ENDPOINTS.ORDER.CHECKOUT(tenantId),
            payload,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return r.data;
    },

    getMyOrders: async (page = 0, size = 10): Promise<OrderPageResponse<OrderDetail>> => {
        const r = await api.get<OrderPageResponse<OrderDetail>>(
            API_ENDPOINTS.ORDER.MY_ORDERS,
            { params: { page, size } },
        );
        return r.data;
    },

    getMyOrderDetail: async (orderId: number): Promise<OrderDetail> => {
        const r = await api.get<OrderDetail>(API_ENDPOINTS.ORDER.MY_ORDER_DETAIL(orderId));
        return r.data;
    },

    cancelOrder: async (orderId: number, reason?: string): Promise<OrderResponse> => {
        const r = await api.post<OrderResponse>(
            API_ENDPOINTS.ORDER.CANCEL(orderId),
            { reason },
        );
        return r.data;
    },

    // ─── İade (return) ───────────────────────────────────────────────────
    requestReturn: async (orderId: number, reasonCode: string, note?: string): Promise<OrderResponse> => {
        const r = await api.post<OrderResponse>(API_ENDPOINTS.ORDER.RETURN(orderId), { reasonCode, note });
        return r.data;
    },

    getTenantReturns: async (tenantId: number): Promise<OrderReturn[]> => {
        const r = await api.get<OrderReturn[]>(API_ENDPOINTS.ORDER.TENANT_RETURNS(tenantId));
        return r.data;
    },

    approveReturn: async (tenantId: number, orderId: number, note?: string): Promise<OrderResponse> => {
        const r = await api.post<OrderResponse>(
            API_ENDPOINTS.ORDER.TENANT_RETURN_APPROVE(tenantId, orderId), { note });
        return r.data;
    },

    rejectReturn: async (tenantId: number, orderId: number, note?: string): Promise<OrderResponse> => {
        const r = await api.post<OrderResponse>(
            API_ENDPOINTS.ORDER.TENANT_RETURN_REJECT(tenantId, orderId), { note });
        return r.data;
    },

    adminGetReturns: async (): Promise<OrderReturn[]> => {
        const r = await api.get<OrderReturn[]>(API_ENDPOINTS.ORDER.ADMIN_RETURNS);
        return r.data;
    },

    adminApproveReturn: async (orderId: number, note?: string): Promise<void> => {
        await api.post(API_ENDPOINTS.ORDER.ADMIN_RETURN_APPROVE(orderId), { note });
    },

    adminRejectReturn: async (orderId: number, note?: string): Promise<void> => {
        await api.post(API_ENDPOINTS.ORDER.ADMIN_RETURN_REJECT(orderId), { note });
    },

    // "Birlikte sıkça alınanlar" — co-purchase ürün id'leri (ürün detay rail'i). Auth gerektirir.
    getFrequentlyBoughtWith: async (productId: number, limit = 10): Promise<number[]> => {
        const r = await api.get<number[]>(
            API_ENDPOINTS.ORDER.FREQUENTLY_BOUGHT_WITH(productId),
            { params: { limit } },
        );
        return r.data ?? [];
    },

    getTenantOrders: async (
        tenantId: number,
        page = 0,
        size = 20,
        status = '',
        q = '',
    ): Promise<OrderPageResponse<OrderDetail>> => {
        const r = await api.get<OrderPageResponse<OrderDetail>>(
            API_ENDPOINTS.ORDER.TENANT_ORDERS(tenantId),
            {
                params: {
                    page,
                    size,
                    ...(status ? { status } : {}),
                    ...(q.trim() ? { q: q.trim() } : {}),
                },
            },
        );
        return r.data;
    },

    updateOrderStatus: async (
        tenantId: number,
        orderId: number,
        status: string,
        trackingNumber?: string,
    ): Promise<OrderResponse> => {
        const r = await api.put<OrderResponse>(
            API_ENDPOINTS.ORDER.UPDATE_STATUS(tenantId, orderId),
            { status, trackingNumber },
        );
        return r.data;
    },

    getTenantAnalytics: async (tenantId: number): Promise<MerchantAnalytics> => {
        const r = await api.get<MerchantAnalytics>(API_ENDPOINTS.ORDER.TENANT_ANALYTICS(tenantId));
        return r.data;
    },

    getTenantProductMetrics: async (tenantId: number, productId: number): Promise<ProductSalesMetrics> => {
        const r = await api.get<ProductSalesMetrics>(
            API_ENDPOINTS.ORDER.TENANT_PRODUCT_METRICS(tenantId, productId),
        );
        return r.data;
    },

    getAdminProductMetrics: async (productId: number): Promise<ProductSalesMetrics> => {
        const r = await api.get<ProductSalesMetrics>(
            API_ENDPOINTS.ORDER.ADMIN_PRODUCT_METRICS(productId),
        );
        return r.data;
    },
};
