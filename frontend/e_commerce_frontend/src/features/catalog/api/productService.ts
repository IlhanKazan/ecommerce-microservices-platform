import { api } from '../../../lib/axios';
import { API_ENDPOINTS } from '../../../config/apiEndpoints';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';
import { asRecord, getString, getNumber } from '../../../utils/normalizers';
import { normalizePage } from '../../../utils/pageResponse';
import type {
    ProductSearchPayload,
    ProductDetail,
    PageResponse,
    ProductSummary,
    ProductReviewDTO,
    ReviewCreateRequest,
    CategoryResponse,
    TenantProductResponse,
    ProductDetailResponse,
    ProductCreateRequest,
    ProductUpdateRequest,
    AutocompleteSuggestion,
    TenantStorefront,
    BrandFacet,
    VariantSummary,
    VariantRequest,
    VariantStock,
} from '../../../types/product';
import type { BasketResponse, AddItemRequest } from '../../../types';

export type SearchPayload = ProductSearchPayload;

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Idempotency key varsa header objesi döner, yoksa boş obje */
function idempotencyHeader(key?: string): Record<string, string> {
    return key ? { [IDEMPOTENCY_KEY_HEADER]: key } : {};
}

/** Backend ProductResponse → ProductCard'ın beklediği ProductSummary (favori listesi için).
 *  Favori endpoint'i stok bilgisi taşımaz; inStock varsayılan true. */
function toProductSummary(raw: unknown): ProductSummary {
    const r = asRecord(raw);
    return {
        id: String(getNumber(r, 'id') ?? 0),
        tenantId: getNumber(r, 'tenantId') ?? 0,
        categoryId: getNumber(r, 'categoryId') ?? null,
        categoryName: getString(r, 'categoryName') ?? null,
        sku: getString(r, 'sku') ?? '',
        name: getString(r, 'name') ?? '',
        brand: getString(r, 'brand') ?? null,
        price: getNumber(r, 'price') ?? 0,
        discountedPrice: getNumber(r, 'discountedPrice') ?? null,
        currency: getString(r, 'currency') ?? 'TRY',
        mainImageUrl: getString(r, 'mainImageUrl') ?? null,
        ratingAverage: getNumber(r, 'ratingAverage') ?? null,
        reviewCount: getNumber(r, 'reviewCount') ?? 0,
        salesStatus: getString(r, 'salesStatus') ?? 'ON_SALE',
        inStock: true,
        tenantName: getString(r, 'tenantName') ?? null,
        tenantLogoUrl: getString(r, 'tenantLogoUrl') ?? null,
    };
}

// ─── Product Service ──────────────────────────────────────────────────────────

export const productService = {

    searchProducts: async (body: ProductSearchPayload): Promise<PageResponse<ProductSummary>> => {
        const response = await api.post<unknown>(
            API_ENDPOINTS.SEARCH.PRODUCTS,
            body,
        );
        return normalizePage<ProductSummary>(response.data);
    },

    // Id listesiyle ürün kartları (son gezilenler, birlikte alınanlar). Backend sırayı korur.
    getProductsByIds: async (ids: number[]): Promise<ProductSummary[]> => {
        if (!ids || ids.length === 0) return [];
        const response = await api.post<ProductSummary[]>(
            API_ENDPOINTS.SEARCH.PRODUCTS_BY_IDS,
            { ids },
        );
        return response.data ?? [];
    },

    // Benzer ürünler (ürün detayı rail'i).
    getSimilarProducts: async (productId: number, size = 10): Promise<ProductSummary[]> => {
        const response = await api.get<ProductSummary[]>(
            API_ENDPOINTS.SEARCH.SIMILAR(productId),
            { params: { size } },
        );
        return response.data ?? [];
    },

    // İlgili ürünler (seed-bazlı) — "Senin İçin" rail'inin fallback'i.
    getRelatedProducts: async (
        seedIds: number[],
        excludeIds: number[] = [],
        size = 12,
    ): Promise<ProductSummary[]> => {
        const response = await api.post<ProductSummary[]>(
            API_ENDPOINTS.SEARCH.RELATED,
            { seedIds, excludeIds, size },
        );
        return response.data ?? [];
    },

    autocomplete: async (q: string, size = 5): Promise<AutocompleteSuggestion[]> => {
        const response = await api.get<AutocompleteSuggestion[]>(
            API_ENDPOINTS.SEARCH.AUTOCOMPLETE,
            { params: { q, size } },
        );
        return response.data;
    },

    getBrandFacets: async (body: ProductSearchPayload): Promise<BrandFacet[]> => {
        const response = await api.post<BrandFacet[]>(API_ENDPOINTS.SEARCH.BRANDS, body);
        return response.data;
    },

    getProductDetail: async (id: number): Promise<ProductDetail> => {
        const response = await api.get<ProductDetail>(API_ENDPOINTS.PRODUCT.BY_ID_PUBLIC(id));
        return response.data;
    },

    // Detay sayfası fiziksel stok göstergesi — kartla aynı ES kaynağı (taze, cache'siz).
    getProductAvailability: async (id: number): Promise<{ inStock: boolean; salesStatus: string | null }> => {
        const response = await api.get<{ inStock: boolean; salesStatus: string | null }>(
            API_ENDPOINTS.SEARCH.AVAILABILITY(id),
        );
        return response.data;
    },

    // Varyant-bazlı canlı stok — detay sayfasında stoksuz varyantı çarpılı göstermek + "Son X adet" için.
    // stock-service authoritative; ürün cache'inden bağımsız taze.
    getVariantStock: async (productIds: number[]): Promise<VariantStock[]> => {
        if (productIds.length === 0) return [];
        const response = await api.post<VariantStock[]>(
            API_ENDPOINTS.STOCK.AVAILABILITY,
            { productIds },
        );
        return response.data;
    },

    // ─── Favorites ───────────────────────────────────────────────────────────

    getFavoriteIds: async (): Promise<number[]> => {
        const response = await api.get<number[]>(API_ENDPOINTS.PRODUCT.FAVORITE_IDS);
        return response.data;
    },

    getFavorites: async (): Promise<ProductSummary[]> => {
        const response = await api.get<unknown[]>(API_ENDPOINTS.PRODUCT.FAVORITES);
        return Array.isArray(response.data) ? response.data.map(toProductSummary) : [];
    },

    addFavorite: async (productId: number): Promise<void> => {
        await api.put(API_ENDPOINTS.PRODUCT.FAVORITE(productId));
    },

    removeFavorite: async (productId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.PRODUCT.FAVORITE(productId));
    },

    // ─── Reviews ─────────────────────────────────────────────────────────────

    getProductReviews: async (
        id: number,
        page = 0,
        size = 10,
    ): Promise<PageResponse<ProductReviewDTO>> => {
        const response = await api.get<unknown>(
            API_ENDPOINTS.PRODUCT.REVIEWS(id),
            { params: { page, size } },
        );
        return normalizePage<ProductReviewDTO>(response.data);
    },

    createReview: async (
        productId: number,
        body: ReviewCreateRequest,
        idempotencyKey: string,
    ): Promise<ProductReviewDTO> => {
        const response = await api.post<ProductReviewDTO>(
            API_ENDPOINTS.PRODUCT.REVIEWS(productId),
            body,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    markReviewHelpful: async (
        productId: number,
        reviewId: number,
        helpful: boolean,
    ): Promise<void> => {
        await api.patch(
            API_ENDPOINTS.PRODUCT.REVIEW_HELPFUL(productId, reviewId),
            null,
            { params: { helpful } },
        );
    },

    deleteReview: async (productId: number, reviewId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.PRODUCT.REVIEW_DELETE(productId, reviewId));
    },

    // ─── Public Tenant Storefront ─────────────────────────────────────────────

    getTenantStorefront: async (tenantId: number): Promise<TenantStorefront> => {
        const response = await api.get<TenantStorefront>(API_ENDPOINTS.TENANT.STOREFRONT(tenantId));
        return response.data;
    },

    // ─── Categories ───────────────────────────────────────────────────────────

    getCategories: async (): Promise<CategoryResponse[]> => {
        const response = await api.get<CategoryResponse[]>(API_ENDPOINTS.CATEGORY.ALL);
        return response.data;
    },

    getCategoryBySlug: async (slug: string): Promise<CategoryResponse> => {
        const response = await api.get<CategoryResponse>(API_ENDPOINTS.CATEGORY.BY_SLUG(slug));
        return response.data;
    },

    // ─── Tenant Product Management ────────────────────────────────────────────

    getTenantProducts: async (
        tenantId: number,
        page = 0,
        size = 20,
        q = '',
        salesStatus = '',
        sort = '',
    ): Promise<PageResponse<TenantProductResponse>> => {
        const response = await api.get<unknown>(
            API_ENDPOINTS.PRODUCT.TENANT_LIST(tenantId),
            {
                params: {
                    page,
                    size,
                    ...(q.trim() ? { q: q.trim() } : {}),
                    ...(salesStatus ? { salesStatus } : {}),
                    ...(sort ? { sort } : {}),
                },
            },
        );
        return normalizePage<TenantProductResponse>(response.data);
    },

    createTenantProduct: async (
        tenantId: number,
        body: ProductCreateRequest,
        idempotencyKey: string,
    ): Promise<TenantProductResponse> => {
        const response = await api.post<TenantProductResponse>(
            API_ENDPOINTS.PRODUCT.TENANT_CREATE(tenantId),
            body,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    updateTenantProduct: async (
        tenantId: number,
        productId: number,
        body: ProductUpdateRequest,
        idempotencyKey: string,
    ): Promise<TenantProductResponse> => {
        const response = await api.put<TenantProductResponse>(
            API_ENDPOINTS.PRODUCT.TENANT_UPDATE(tenantId, productId),
            body,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    deleteTenantProduct: async (tenantId: number, productId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.PRODUCT.TENANT_DELETE(tenantId, productId));
    },

    // ─── Varyant (child product) yönetimi ──────────────────────────────
    getVariants: async (tenantId: number, productId: number): Promise<VariantSummary[]> => {
        const response = await api.get<VariantSummary[]>(
            API_ENDPOINTS.PRODUCT.TENANT_VARIANTS(tenantId, productId),
        );
        return response.data;
    },

    createVariant: async (
        tenantId: number,
        productId: number,
        body: VariantRequest,
        idempotencyKey: string,
    ): Promise<VariantSummary> => {
        const response = await api.post<VariantSummary>(
            API_ENDPOINTS.PRODUCT.TENANT_VARIANTS(tenantId, productId),
            body,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    updateVariant: async (
        tenantId: number,
        variantId: number,
        body: VariantRequest,
        idempotencyKey: string,
    ): Promise<VariantSummary> => {
        const response = await api.put<VariantSummary>(
            API_ENDPOINTS.PRODUCT.TENANT_VARIANT_BY_ID(tenantId, variantId),
            body,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    deleteVariant: async (tenantId: number, variantId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.PRODUCT.TENANT_VARIANT_BY_ID(tenantId, variantId));
    },

    // Matris üretici: tek istekte N varyant. Atomik — biri patlarsa hiçbiri eklenmez.
    createVariantsBatch: async (
        tenantId: number,
        productId: number,
        variants: VariantRequest[],
        idempotencyKey: string,
    ): Promise<VariantSummary[]> => {
        const response = await api.post<VariantSummary[]>(
            API_ENDPOINTS.PRODUCT.TENANT_VARIANTS_BATCH(tenantId, productId),
            { variants },
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    updateSalesStatus: async (
        tenantId: number,
        productId: number,
        status: 'ON_SALE' | 'OUT_OF_STOCK' | 'COMING_SOON',
    ): Promise<void> => {
        await api.patch(
            API_ENDPOINTS.PRODUCT.TENANT_SALES_STATUS(tenantId, productId),
            null,
            { params: { status } },
        );
    },

    setFeatured: async (
        tenantId: number,
        productId: number,
        featured: boolean,
    ): Promise<void> => {
        await api.patch(
            API_ENDPOINTS.PRODUCT.TENANT_FEATURED(tenantId, productId),
            null,
            { params: { featured } },
        );
    },

    addSellerResponse: async (
        tenantId: number,
        productId: number,
        reviewId: number,
        response: string,
    ): Promise<void> => {
        await api.post(
            API_ENDPOINTS.PRODUCT.SELLER_RESPONSE(tenantId, productId, reviewId),
            { response },
        );
    },

    uploadReviewImage: async (productId: number, file: File): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post<{ url: string }>(
            API_ENDPOINTS.PRODUCT.REVIEW_IMAGE_UPLOAD(productId),
            formData,
            { headers: { 'Content-Type': undefined } },
        );
        return response.data.url;
    },

    uploadProductImage: async (tenantId: number, file: File): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post<{ url: string }>(
            API_ENDPOINTS.PRODUCT.TENANT_IMAGE_UPLOAD(tenantId),
            formData,
            { headers: { 'Content-Type': undefined } },
        );
        return response.data.url;
    },

    getTenantProductById: async (
        tenantId: number,
        productId: number,
    ): Promise<TenantProductResponse> => {
        const response = await api.get<TenantProductResponse>(
            API_ENDPOINTS.PRODUCT.TENANT_BY_ID(tenantId, productId),
        );
        return response.data;
    },

    getTenantProductDetail: async (
        tenantId: number,
        productId: number,
    ): Promise<ProductDetailResponse> => {
        const response = await api.get<ProductDetailResponse>(
            API_ENDPOINTS.PRODUCT.TENANT_DETAIL(tenantId, productId),
        );
        return response.data;
    },
};

// ─── Basket Service ───────────────────────────────────────────────────────────

export const basketService = {

    getCart: async (): Promise<BasketResponse> => {
        const response = await api.get<BasketResponse>(API_ENDPOINTS.BASKET.GET);
        return response.data;
    },

    /** idempotencyKey — useAddToBasket hook'undan geliyor */
    addToCart: async (payload: AddItemRequest, idempotencyKey: string): Promise<void> => {
        await api.post(
            API_ENDPOINTS.BASKET.ADD,
            payload,
            { headers: idempotencyHeader(idempotencyKey) },
        );
    },

    /** Guest sepetini hesap sepetine birleştir — tek atomik çağrı (miktarlar toplanır). */
    mergeCart: async (items: AddItemRequest[]): Promise<void> => {
        await api.post(API_ENDPOINTS.BASKET.MERGE, { items });
    },

    removeItem: async (productId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.BASKET.REMOVE_ITEM(productId));
    },

    removeFromCart: async (productId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.BASKET.REMOVE_ITEM(productId));
    },

    updateCartItem: async (payload: { productId: number; quantity: number }): Promise<void> => {
        await api.patch(API_ENDPOINTS.BASKET.UPDATE_ITEM(payload.productId), { quantity: payload.quantity });
    },

    clearBasket: async (): Promise<void> => {
        await api.delete(API_ENDPOINTS.BASKET.CLEAR);
    },
};