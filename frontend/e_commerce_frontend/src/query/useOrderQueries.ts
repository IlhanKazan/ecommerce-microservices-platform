import { useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { orderService } from '../features/checkout/api/orderService';
import { generateIdempotencyKey } from '../utils/idempotencyUtils';
import { useClearBasket } from './useBasketQueries';
import { useCartStore } from '../store/useCartStore';
import { useAuthStore } from '../store/useAuthStore';
import type { CheckoutRequest } from '../types/order';

// Döngüsel import önlemek için queryKeys.ts kullanılmıyor, inline tanımlandı
const ORDER_KEYS = {
    myOrders: (page: number, size: number) => ['my-orders', page, size] as const,
    orderDetail: (orderId: number) => ['order-detail', orderId] as const,
    tenantOrders: (tenantId: number, page: number, size: number) =>
        ['tenant-orders', tenantId, page, size] as const,
};

export const useGetMyOrders = (page = 0, size = 10) => {
    const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
    return useQuery({
        queryKey: ORDER_KEYS.myOrders(page, size),
        queryFn: () => orderService.getMyOrders(page, size),
        enabled: isAuthenticated,
        staleTime: 30_000,
    });
};

export const useGetMyOrderDetail = (orderId: number | null) => {
    return useQuery({
        queryKey: ORDER_KEYS.orderDetail(orderId ?? 0),
        queryFn: () => orderService.getMyOrderDetail(orderId!),
        enabled: !!orderId,
        staleTime: 30_000,
    });
};

export const useCreateOrder = () => {
    const navigate = useNavigate();
    const { mutate: clearBasket } = useClearBasket();
    const clearCart = useCartStore((s) => s.clearCart);
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: ({
            tenantId,
            payload,
        }: {
            tenantId: number;
            payload: CheckoutRequest;
        }) => orderService.createOrder(tenantId, payload, idempotencyKey.current),
        onSuccess: (data) => {
            idempotencyKey.current = generateIdempotencyKey();
            clearBasket();
            clearCart();
            navigate('/user/orders', { state: { newOrderId: data.orderId, success: true } });
        },
    });
};

export const useCancelOrder = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ orderId, reason }: { orderId: number; reason?: string }) =>
            orderService.cancelOrder(orderId, reason),
        onSuccess: (_data, { orderId }) => {
            queryClient.invalidateQueries({ queryKey: ['my-orders'] });
            queryClient.invalidateQueries({ queryKey: ORDER_KEYS.orderDetail(orderId) });
        },
    });
};

export const useGetTenantOrders = (tenantId: number | null, page = 0, size = 20) => {
    return useQuery({
        queryKey: ORDER_KEYS.tenantOrders(tenantId ?? 0, page, size),
        queryFn: () => orderService.getTenantOrders(tenantId!, page, size),
        enabled: !!tenantId,
        staleTime: 30_000,
    });
};

export const useUpdateOrderStatus = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({
            tenantId,
            orderId,
            status,
            trackingNumber,
        }: {
            tenantId: number;
            orderId: number;
            status: string;
            trackingNumber?: string;
        }) => orderService.updateOrderStatus(tenantId, orderId, status, trackingNumber),
        onSuccess: (_data, { tenantId }) => {
            queryClient.invalidateQueries({ queryKey: ['tenant-orders', tenantId] });
        },
    });
};
