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
    tenantOrders: (tenantId: number, page: number, size: number, status = '', q = '') =>
        ['tenant-orders', tenantId, page, size, status, q] as const,
    tenantAnalytics: (tenantId: number) => ['tenant-analytics', tenantId] as const,
    tenantProductMetrics: (tenantId: number, productId: number) =>
        ['tenant-product-metrics', tenantId, productId] as const,
    adminProductMetrics: (productId: number) => ['admin-product-metrics', productId] as const,
    tenantReturns: (tenantId: number) => ['tenant-returns', tenantId] as const,
    adminReturns: ['admin-returns'] as const,
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

// ─── İade (return) ───────────────────────────────────────────────────────────

// Müşteri iade talebi
export const useRequestReturn = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ orderId, reasonCode, note }: { orderId: number; reasonCode: string; note?: string }) =>
            orderService.requestReturn(orderId, reasonCode, note),
        onSuccess: (_data, { orderId }) => {
            queryClient.invalidateQueries({ queryKey: ['my-orders'] });
            queryClient.invalidateQueries({ queryKey: ORDER_KEYS.orderDetail(orderId) });
        },
    });
};

// Merchant — bekleyen iadeler
export const useGetTenantReturns = (tenantId: number | null) => {
    return useQuery({
        queryKey: ORDER_KEYS.tenantReturns(tenantId ?? 0),
        queryFn: () => orderService.getTenantReturns(tenantId!),
        enabled: !!tenantId,
        staleTime: 30_000,
    });
};

export const useApproveReturn = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ orderId, note }: { orderId: number; note?: string }) =>
            orderService.approveReturn(tenantId, orderId, note),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ORDER_KEYS.tenantReturns(tenantId) });
            queryClient.invalidateQueries({ queryKey: ['tenant-orders', tenantId] });
        },
    });
};

export const useRejectReturn = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ orderId, note }: { orderId: number; note?: string }) =>
            orderService.rejectReturn(tenantId, orderId, note),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ORDER_KEYS.tenantReturns(tenantId) });
            queryClient.invalidateQueries({ queryKey: ['tenant-orders', tenantId] });
        },
    });
};

// Admin — override
export const useGetAdminReturns = () => {
    return useQuery({
        queryKey: ORDER_KEYS.adminReturns,
        queryFn: () => orderService.adminGetReturns(),
        staleTime: 30_000,
    });
};

export const useAdminApproveReturn = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ orderId, note }: { orderId: number; note?: string }) =>
            orderService.adminApproveReturn(orderId, note),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ORDER_KEYS.adminReturns });
            queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
        },
    });
};

export const useAdminRejectReturn = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ orderId, note }: { orderId: number; note?: string }) =>
            orderService.adminRejectReturn(orderId, note),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ORDER_KEYS.adminReturns });
            queryClient.invalidateQueries({ queryKey: ['admin-orders'] });
        },
    });
};

export const useGetTenantOrders = (
    tenantId: number | null,
    page = 0,
    size = 20,
    status = '',
    q = '',
) => {
    return useQuery({
        queryKey: ORDER_KEYS.tenantOrders(tenantId ?? 0, page, size, status, q),
        queryFn: () => orderService.getTenantOrders(tenantId!, page, size, status, q),
        enabled: !!tenantId,
        staleTime: 30_000,
        placeholderData: (prev) => prev,
    });
};

// "Birlikte sıkça alınanlar" — co-purchase ürün id'leri (ürün detay rail'i için). Auth gerektirir.
export const useFrequentlyBoughtWith = (productId: number, enabled: boolean, limit = 10) => {
    return useQuery({
        queryKey: ['frequently-bought-with', productId, limit],
        queryFn: () => orderService.getFrequentlyBoughtWith(productId, limit),
        enabled: enabled && !!productId,
        staleTime: 1000 * 60 * 5,
    });
};

export const useGetTenantAnalytics = (tenantId: number | null) => {
    return useQuery({
        queryKey: ORDER_KEYS.tenantAnalytics(tenantId ?? 0),
        queryFn: () => orderService.getTenantAnalytics(tenantId!),
        enabled: !!tenantId,
        staleTime: 60_000,
    });
};

// Tek ürün satış metriği — merchant (tenant-scope). enabled modal açıkken.
export const useTenantProductMetrics = (
    tenantId: number | null,
    productId: number | null,
    enabled = true,
) => {
    return useQuery({
        queryKey: ORDER_KEYS.tenantProductMetrics(tenantId ?? 0, productId ?? 0),
        queryFn: () => orderService.getTenantProductMetrics(tenantId!, productId!),
        enabled: enabled && !!tenantId && !!productId,
        staleTime: 60_000,
    });
};

// Tek ürün satış metriği — admin (platform geneli).
export const useAdminProductMetrics = (productId: number | null, enabled = true) => {
    return useQuery({
        queryKey: ORDER_KEYS.adminProductMetrics(productId ?? 0),
        queryFn: () => orderService.getAdminProductMetrics(productId!),
        enabled: enabled && !!productId,
        staleTime: 60_000,
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
