import { api } from '../../../lib/axios';
import { API_ENDPOINTS } from '../../../config/apiEndpoints';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';
import type { CheckoutRequest, OrderResponse, OrderDetail, OrderPageResponse } from '../../../types/order';

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

    getTenantOrders: async (
        tenantId: number,
        page = 0,
        size = 20,
    ): Promise<OrderPageResponse<OrderDetail>> => {
        const r = await api.get<OrderPageResponse<OrderDetail>>(
            API_ENDPOINTS.ORDER.TENANT_ORDERS(tenantId),
            { params: { page, size } },
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
};
