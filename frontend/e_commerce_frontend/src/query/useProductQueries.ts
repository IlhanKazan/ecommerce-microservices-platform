import { useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    productService,
    basketService,
    type SearchPayload,
} from '../features/catalog/api/productService';
import { tenantService } from '../features/tenant/api/tenantService';
import { QueryKeys } from './queryKeys';
import { generateIdempotencyKey, IDEMPOTENCY_KEY_HEADER } from '../utils/idempotencyUtils';
import type { ReviewCreateRequest } from '../types/product';

// ─── Catalog ──────────────────────────────────────────────────────────────────

export const useSearchProducts = (body: SearchPayload) => {
    return useQuery({
        queryKey: QueryKeys.SEARCH_PRODUCTS(body),
        queryFn: () => productService.searchProducts(body),
        staleTime: 1000 * 60 * 2,
    });
};

export const useGetProductDetail = (productId: number) => {
    return useQuery({
        queryKey: QueryKeys.PRODUCT_DETAIL(productId),
        queryFn: () => productService.getProductDetail(productId),
        enabled: !!productId,
    });
};

// ─── Reviews ──────────────────────────────────────────────────────────────────

export const useGetProductReviews = (productId: number, page = 0, size = 10) => {
    return useQuery({
        queryKey: QueryKeys.PRODUCT_REVIEWS(productId, page, size),
        queryFn: () => productService.getProductReviews(productId, page, size),
        enabled: !!productId,
    });
};

/**
 * Yorum oluşturma — idempotency key ile.
 *
 * Kullanıcı "Gönder"e bastı → ağ koptu → tekrar bastı senaryosunda
 * backend aynı key'i görünce ikinci isteği işlemez (Redis dedupe).
 *
 * Key lifecycle:
 *   - Hook mount'ta bir kez üretilir
 *   - onSuccess'te yenilenir (bir sonraki yorum için farklı key hazır)
 *   - retry'da aynı key kalır → Redis dedupe çalışır
 */
export const useCreateReview = (productId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (body: ReviewCreateRequest) =>
            productService.createReview(productId, body, idempotencyKey.current),
        onSuccess: () => {
            // Başarılı → bir sonraki işlem için yeni key
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ['product-reviews', productId] });
            queryClient.invalidateQueries({ queryKey: QueryKeys.PRODUCT_DETAIL(productId) });
        },
        retry: 2,
    });
};

export const useMarkReviewHelpful = (productId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ reviewId, helpful }: { reviewId: number; helpful: boolean }) =>
            productService.markReviewHelpful(productId, reviewId, helpful),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['product-reviews', productId] });
        },
    });
};

export const useDeleteReview = (productId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (reviewId: number) => productService.deleteReview(productId, reviewId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['product-reviews', productId] });
            queryClient.invalidateQueries({ queryKey: QueryKeys.PRODUCT_DETAIL(productId) });
        },
    });
};

// ─── Categories ───────────────────────────────────────────────────────────────

export const useGetCategories = () => {
    return useQuery({
        queryKey: QueryKeys.CATEGORIES,
        queryFn: productService.getCategories,
        staleTime: 1000 * 60 * 30,
    });
};

// ─── Tenant Product Management ────────────────────────────────────────────────

export const useGetTenantProducts = (tenantId: number, page = 0, size = 20) => {
    return useQuery({
        queryKey: QueryKeys.TENANT_PRODUCTS(tenantId, page, size),
        queryFn: () => productService.getTenantProducts(tenantId, page, size),
        enabled: !!tenantId,
    });
};

export const useCreateTenantProduct = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (body: any) =>
            productService.createTenantProduct(tenantId, body, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ['tenant-products', tenantId] });
        },
        retry: 1,
    });
};

export const useUpdateTenantProduct = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: ({ productId, body }: { productId: number; body: any }) =>
            productService.updateTenantProduct(tenantId, productId, body, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ['tenant-products', tenantId] });
        },
        retry: 1,
    });
};

export const useDeleteTenantProduct = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (productId: number) =>
            productService.deleteTenantProduct(tenantId, productId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant-products', tenantId] });
        },
    });
};

export const useUpdateSalesStatus = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({
                         productId,
                         status,
                     }: {
            productId: number;
            status: 'ON_SALE' | 'OUT_OF_STOCK' | 'COMING_SOON';
        }) => productService.updateSalesStatus(tenantId, productId, status),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant-products', tenantId] });
        },
    });
};

export function useGetTenantProductById(
    tenantId: number,
    productId: number | null,
    enabled: boolean,
) {
    return useQuery({
        queryKey: ['tenant-product-detail', tenantId, productId],
        queryFn: () => productService.getTenantProductById(tenantId, productId!),
        enabled: !!tenantId && !!productId && enabled,
        staleTime: 0, // edit her açılışta taze data
    });
}

// ─── Warehouse ────────────────────────────────────────────────────────────────

export const useGetWarehouses = (tenantId: number) => {
    return useQuery({
        queryKey: QueryKeys.WAREHOUSES(tenantId),
        queryFn: () => tenantService.getWarehouses(tenantId),
        enabled: !!tenantId,
    });
};

export const useCreateWarehouse = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (payload: { code: string; name: string; locationDetails: string }) =>
            tenantService.createWarehouse(tenantId, payload, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
        },
        retry: 1,
    });
};

/**
 * Stok ekleme — en kritik idempotency noktası.
 * Duplicate stok girişi direkt envanter hatasına yol açar.
 */
export const useAddManualStock = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (payload: { warehouseId: number; productId: number; amount: number }) =>
            tenantService.addManualStock(tenantId, payload, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
            queryClient.invalidateQueries({ queryKey: ['searchProducts'] });
            queryClient.invalidateQueries({ queryKey: ['productDetail'] });
        },
        // Stok için retry — ağ hatası olursa aynı key ile tekrar dene
        retry: 2,
    });
};

// ─── Cart (basket) ────────────────────────────────────────────────────────────

export const useAddToCart = () => {
    return useMutation({ mutationFn: basketService.addToCart });
};

export const useGetCart = () => {
    return useQuery({
        queryKey: QueryKeys.CART,
        queryFn: () => basketService.getCart(),
        staleTime: 1000 * 60 * 5,
    });
};

export const useUpdateCartItem = () => {
    return useMutation({ mutationFn: basketService.updateCartItem });
};

export const useRemoveFromCart = () => {
    return useMutation({ mutationFn: basketService.removeFromCart });
};

// ─── Re-export edilmemiş utility (hook dışı kullanım için) ────────────────────
export { IDEMPOTENCY_KEY_HEADER };