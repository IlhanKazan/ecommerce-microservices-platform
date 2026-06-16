import { useRef } from 'react';
import { useMutation, useQuery, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import {
    adminService, type AdminStoreQuery, type AdminOrderQuery, type AdminUserQuery,
    type AdminTransactionQuery, type AdminProductQueryParams,
} from '../features/admin/api/adminService';
import { generateIdempotencyKey } from '../utils/idempotencyUtils';
import type { CreateCategoryRequest, UpdateCategoryRequest } from '../types/admin';

export const ADMIN_QUERY_KEYS = {
    stores: (query: AdminStoreQuery) => ['admin', 'stores', query] as const,
    storesAll: ['admin', 'stores'] as const,
    storeDetail: (id: number) => ['admin', 'store', id] as const,
    categories: ['admin', 'categories'] as const,
    orders: (query: AdminOrderQuery) => ['admin', 'orders', query] as const,
    orderDetail: (id: number) => ['admin', 'order', id] as const,
    orderStats: ['admin', 'order-stats'] as const,
    users: (query: AdminUserQuery) => ['admin', 'users', query] as const,
    usersAll: ['admin', 'users'] as const,
    paymentStats: ['admin', 'payment-stats'] as const,
    productStats: ['admin', 'product-stats'] as const,
    transactions: (query: AdminTransactionQuery) => ['admin', 'transactions', query] as const,
    products: (query: AdminProductQueryParams) => ['admin', 'products', query] as const,
    productsAll: ['admin', 'products'] as const,
};

// ── Stores ──────────────────────────────────────────────────────────────

export const useAdminStores = (query: AdminStoreQuery) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.stores(query),
        queryFn: () => adminService.getStores(query),
        placeholderData: keepPreviousData,
        staleTime: 1000 * 30,
    });

export const useAdminStoreDetail = (tenantId: number | null) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.storeDetail(tenantId ?? 0),
        queryFn: () => adminService.getStoreDetail(tenantId as number),
        enabled: tenantId != null,
    });

export const useSuspendStore = () => {
    const queryClient = useQueryClient();
    const keyRef = useRef<string>(generateIdempotencyKey());
    return useMutation({
        mutationFn: (tenantId: number) => adminService.suspendStore(tenantId, keyRef.current),
        onSuccess: () => {
            keyRef.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.storesAll });
        },
    });
};

export const useReactivateStore = () => {
    const queryClient = useQueryClient();
    const keyRef = useRef<string>(generateIdempotencyKey());
    return useMutation({
        mutationFn: (tenantId: number) => adminService.reactivateStore(tenantId, keyRef.current),
        onSuccess: () => {
            keyRef.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.storesAll });
        },
    });
};

// ── Categories ──────────────────────────────────────────────────────────

export const useAdminCategories = () =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.categories,
        queryFn: () => adminService.getCategories(),
        staleTime: 1000 * 60,
    });

export const useCreateCategory = () => {
    const queryClient = useQueryClient();
    const keyRef = useRef<string>(generateIdempotencyKey());
    return useMutation({
        mutationFn: (payload: CreateCategoryRequest) => adminService.createCategory(payload, keyRef.current),
        onSuccess: () => {
            keyRef.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.categories });
        },
    });
};

export const useUpdateCategory = () => {
    const queryClient = useQueryClient();
    const keyRef = useRef<string>(generateIdempotencyKey());
    return useMutation({
        mutationFn: ({ id, payload }: { id: number; payload: UpdateCategoryRequest }) =>
            adminService.updateCategory(id, payload, keyRef.current),
        onSuccess: () => {
            keyRef.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.categories });
        },
    });
};

export const useSetCategoryStatus = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, active }: { id: number; active: boolean }) =>
            adminService.setCategoryStatus(id, active),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.categories }),
    });
};

export const useDeleteCategory = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: number) => adminService.deleteCategory(id),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.categories }),
    });
};

export const useUploadCategoryImage = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, file }: { id: number; file: File }) => adminService.uploadCategoryImage(id, file),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.categories }),
    });
};

// ── Orders ──────────────────────────────────────────────────────────────

export const useAdminOrders = (query: AdminOrderQuery) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.orders(query),
        queryFn: () => adminService.getOrders(query),
        placeholderData: keepPreviousData,
        staleTime: 1000 * 30,
    });

export const useAdminOrderDetail = (orderId: number | null) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.orderDetail(orderId ?? 0),
        queryFn: () => adminService.getOrderDetail(orderId as number),
        enabled: orderId != null,
    });

export const useAdminOrderStats = () =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.orderStats,
        queryFn: () => adminService.getOrderStats(),
        staleTime: 1000 * 60,
    });

// ── Users ───────────────────────────────────────────────────────────────

export const useAdminUsers = (query: AdminUserQuery) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.users(query),
        queryFn: () => adminService.getUsers(query),
        placeholderData: keepPreviousData,
        staleTime: 1000 * 30,
    });

export const useSetUserStatus = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ id, active }: { id: number; active: boolean }) => adminService.setUserStatus(id, active),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.usersAll }),
    });
};

// ── Gelir / Overview ──────────────────────────────────────────────────────

export const useAdminPaymentStats = () =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.paymentStats,
        queryFn: () => adminService.getPaymentStats(),
        staleTime: 1000 * 60,
    });

export const useAdminProductStats = () =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.productStats,
        queryFn: () => adminService.getProductStats(),
        staleTime: 1000 * 60,
    });

// ── Transactions ──────────────────────────────────────────────────────────

export const useAdminTransactions = (query: AdminTransactionQuery) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.transactions(query),
        queryFn: () => adminService.getTransactions(query),
        placeholderData: keepPreviousData,
        staleTime: 1000 * 30,
    });

// ── Ürünler ───────────────────────────────────────────────────────────────

export const useAdminProducts = (query: AdminProductQueryParams) =>
    useQuery({
        queryKey: ADMIN_QUERY_KEYS.products(query),
        queryFn: () => adminService.getProducts(query),
        placeholderData: keepPreviousData,
        staleTime: 1000 * 30,
    });

export const useDeactivateProduct = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: number) => adminService.deactivateProduct(id),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.productsAll }),
    });
};

export const useRemoveProduct = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (id: number) => adminService.removeProduct(id),
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ADMIN_QUERY_KEYS.productsAll }),
    });
};

// ── Sistem / Bakım ──────────────────────────────────────────────────────────

export const useReindexProducts = () =>
    useMutation({ mutationFn: () => adminService.reindexProducts() });

export const useResyncStocks = () =>
    useMutation({ mutationFn: () => adminService.resyncStocks() });

export const useReconcileReservations = () =>
    useMutation({ mutationFn: () => adminService.reconcileReservations() });

export const useClearCaches = () =>
    useMutation({ mutationFn: () => adminService.clearCaches() });
