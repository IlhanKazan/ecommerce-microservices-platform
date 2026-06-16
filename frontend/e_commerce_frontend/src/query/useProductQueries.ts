import { useRef } from 'react';
import { useQuery, useMutation, useQueryClient, keepPreviousData } from '@tanstack/react-query';
import {
    productService,
    basketService,
    type SearchPayload,
} from '../features/catalog/api/productService';
import { tenantService } from '../features/tenant/api/tenantService';
import { QueryKeys } from './queryKeys';
import { generateIdempotencyKey, IDEMPOTENCY_KEY_HEADER } from '../utils/idempotencyUtils';
import type { ReviewCreateRequest, ProductCreateRequest, ProductUpdateRequest, VariantRequest } from '../types/product';

// ─── Catalog ──────────────────────────────────────────────────────────────────

export const useAutocomplete = (q: string) => {
    return useQuery({
        queryKey: ['autocomplete', q],
        queryFn: () => productService.autocomplete(q),
        enabled: q.trim().length >= 2,
        staleTime: 1000 * 30,
        placeholderData: [],
    });
};

export const useSearchProducts = (body: SearchPayload) => {
    return useQuery({
        queryKey: QueryKeys.SEARCH_PRODUCTS(body),
        queryFn: () => productService.searchProducts(body),
        staleTime: 1000 * 60 * 2,
        // Sayfa/filtre/sıralama değişiminde eski sonuçları ekranda tut → skeleton flicker yok
        placeholderData: keepPreviousData,
    });
};

// Id listesiyle ürün hidrasyonu (son gezilenler, birlikte alınanlar rail'leri).
export const useGetProductsByIds = (ids: number[]) => {
    return useQuery({
        queryKey: ['products-by-ids', ids],
        queryFn: () => productService.getProductsByIds(ids),
        enabled: ids.length > 0,
        staleTime: 1000 * 60 * 2,
    });
};

// Benzer ürünler — ürün detay rail'i.
export const useSimilarProducts = (productId: number, size = 10) => {
    return useQuery({
        queryKey: ['similar-products', productId, size],
        queryFn: () => productService.getSimilarProducts(productId, size),
        enabled: !!productId,
        staleTime: 1000 * 60 * 5,
    });
};

// İlgili ürünler (seed-bazlı) — "Senin İçin" rail'i. seedIds boşsa sorgu kapalı.
export const useRelatedProducts = (seedIds: number[], excludeIds: number[] = [], size = 12) => {
    return useQuery({
        queryKey: ['related-products', seedIds, excludeIds, size],
        queryFn: () => productService.getRelatedProducts(seedIds, excludeIds, size),
        enabled: seedIds.length > 0,
        staleTime: 1000 * 60 * 2,
    });
};

export const useBrandFacets = (body: SearchPayload) => {
    return useQuery({
        queryKey: ['brandFacets', body],
        queryFn: () => productService.getBrandFacets(body),
        staleTime: 1000 * 60 * 2,
        placeholderData: keepPreviousData,
    });
};

export const useGetProductDetail = (productId: number) => {
    return useQuery({
        queryKey: QueryKeys.PRODUCT_DETAIL(productId),
        queryFn: () => productService.getProductDetail(productId),
        enabled: !!productId,
    });
};

// Detay sayfası fiziksel stok durumu — kartla aynı ES kaynağı; product cache'inden bağımsız taze.
export const useProductAvailability = (productId: number) => {
    return useQuery({
        queryKey: ['product-availability', productId],
        queryFn: () => productService.getProductAvailability(productId),
        enabled: !!productId,
        staleTime: 1000 * 30,
    });
};

/**
 * Varyant-bazlı canlı stok — detay sayfasında stoksuz varyantı çarpılı göstermek + "Son X adet".
 * productIds sıralanarak key'lenir → aynı küme için tek cache girişi.
 */
export const useVariantStock = (productIds: number[]) => {
    const sortedIds = [...productIds].sort((a, b) => a - b);
    return useQuery({
        queryKey: ['variant-stock', sortedIds],
        queryFn: () => productService.getVariantStock(productIds),
        enabled: productIds.length > 0,
        staleTime: 1000 * 20,
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

// ─── Tenant Storefront ────────────────────────────────────────────────────────

export const useGetTenantStorefront = (tenantId: number) => {
    return useQuery({
        queryKey: ['tenant-storefront', tenantId],
        queryFn: () => productService.getTenantStorefront(tenantId),
        enabled: !!tenantId,
        staleTime: 1000 * 60 * 5,
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

export const useGetTenantProducts = (
    tenantId: number,
    page = 0,
    size = 20,
    q = '',
    salesStatus = '',
    sort = '',
) => {
    return useQuery({
        queryKey: QueryKeys.TENANT_PRODUCTS(tenantId, page, size, q, salesStatus, sort),
        queryFn: () => productService.getTenantProducts(tenantId, page, size, q, salesStatus, sort),
        enabled: !!tenantId,
        placeholderData: (prev) => prev,
    });
};

export const useCreateTenantProduct = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (body: ProductCreateRequest) =>
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
        mutationFn: ({ productId, body }: { productId: number; body: ProductUpdateRequest }) =>
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

// ─── Varyant (child product) yönetimi ──────────────────────────────────────────

export const useGetVariants = (tenantId: number, productId: number | null) => {
    return useQuery({
        queryKey: ['tenant-variants', tenantId, productId],
        queryFn: () => productService.getVariants(tenantId, productId!),
        enabled: !!tenantId && !!productId,
    });
};

export const useCreateVariant = (tenantId: number, productId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (body: VariantRequest) =>
            productService.createVariant(tenantId, productId, body, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ['tenant-variants', tenantId, productId] });
        },
        retry: 1,
    });
};

/** Matris üretici: tek mutation'da N varyant oluşturur. */
export const useCreateVariantsBatch = (tenantId: number, productId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (variants: VariantRequest[]) =>
            productService.createVariantsBatch(tenantId, productId, variants, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ['tenant-variants', tenantId, productId] });
        },
        retry: 1,
    });
};

export const useUpdateVariant = (tenantId: number, productId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: ({ variantId, body }: { variantId: number; body: VariantRequest }) =>
            productService.updateVariant(tenantId, variantId, body, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: ['tenant-variants', tenantId, productId] });
        },
        retry: 1,
    });
};

export const useDeleteVariant = (tenantId: number, productId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (variantId: number) => productService.deleteVariant(tenantId, variantId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['tenant-variants', tenantId, productId] });
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

export const useSetFeatured = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: ({ productId, featured }: { productId: number; featured: boolean }) =>
            productService.setFeatured(tenantId, productId, featured),
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
        queryFn: () => productService.getTenantProductDetail(tenantId, productId!),
        enabled: !!tenantId && !!productId && enabled,
        staleTime: 0,
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

export const useUpdateWarehouse = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: ({
                         warehouseId,
                         payload,
                     }: {
            warehouseId: number;
            payload: { name: string; locationDetails: string };
        }) => tenantService.updateWarehouse(tenantId, warehouseId, payload, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
        },
        retry: 1,
    });
};

export const useSetWarehouseStatus = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: ({ warehouseId, active }: { warehouseId: number; active: boolean }) =>
            tenantService.setWarehouseStatus(tenantId, warehouseId, active, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
            queryClient.invalidateQueries({ queryKey: QueryKeys.TENANT_STOCKS(tenantId) });
        },
        retry: 1,
    });
};

export const useDeleteWarehouse = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (warehouseId: number) => tenantService.deleteWarehouse(tenantId, warehouseId),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
            queryClient.invalidateQueries({ queryKey: QueryKeys.TENANT_STOCKS(tenantId) });
        },
    });
};

/**
 * Stok ekleme — en kritik idempotency noktası.
 * Duplicate stok girişi direkt envanter hatasına yol açar.
 */
export const useGetTenantStocks = (tenantId: number) => {
    return useQuery({
        queryKey: QueryKeys.TENANT_STOCKS(tenantId),
        queryFn: () => tenantService.getStockSummary(tenantId),
        enabled: !!tenantId,
        staleTime: 1000 * 30,
    });
};

export const useAddManualStock = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (payload: { warehouseId: number; productId: number; amount: number }) =>
            tenantService.addManualStock(tenantId, payload, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
            queryClient.invalidateQueries({ queryKey: QueryKeys.TENANT_STOCKS(tenantId) });
            queryClient.invalidateQueries({ queryKey: ['searchProducts'] });
            queryClient.invalidateQueries({ queryKey: ['productDetail'] });
        },
        // Stok için retry — ağ hatası olursa aynı key ile tekrar dene
        retry: 2,
    });
};

/** Varyant matris akışında oluşturulan varyantlara tek depoya toplu ilk stok girişi. */
export const useAddManualStockBatch = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (payload: { warehouseId: number; items: { productId: number; amount: number }[] }) =>
            tenantService.addManualStockBatch(tenantId, payload, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
            queryClient.invalidateQueries({ queryKey: QueryKeys.TENANT_STOCKS(tenantId) });
            queryClient.invalidateQueries({ queryKey: ['searchProducts'] });
            queryClient.invalidateQueries({ queryKey: ['productDetail'] });
        },
        retry: 2,
    });
};

export const useRemoveManualStock = (tenantId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (payload: { warehouseId: number; productId: number; amount: number }) =>
            tenantService.removeManualStock(tenantId, payload, idempotencyKey.current),
        onSuccess: () => {
            idempotencyKey.current = generateIdempotencyKey();
            queryClient.invalidateQueries({ queryKey: QueryKeys.WAREHOUSES(tenantId) });
            queryClient.invalidateQueries({ queryKey: QueryKeys.TENANT_STOCKS(tenantId) });
            queryClient.invalidateQueries({ queryKey: ['searchProducts'] });
            queryClient.invalidateQueries({ queryKey: ['productDetail'] });
        },
        retry: 2,
    });
};

export const useUpdateLowStockThreshold = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (payload: { warehouseId: number; productId: number; threshold: number }) =>
            tenantService.updateLowStockThreshold(tenantId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: QueryKeys.TENANT_STOCKS(tenantId) });
        },
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