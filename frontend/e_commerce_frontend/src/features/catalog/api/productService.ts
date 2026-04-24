import { api } from '../../../lib/axios';
import { API_ENDPOINTS } from '../../../config/apiEndpoints';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';
import type {
    ProductSearchPayload,
    ProductDetail,
    PageResponse,
    ProductSummary,
    ProductReviewDTO,
    ReviewCreateRequest,
    CategoryResponse,
    TenantProductResponse,
    ProductCreateRequest,
    ProductUpdateRequest,
} from '../../../types/product';
import type { BasketResponse, AddItemRequest } from '../../../types';

export type SearchPayload = ProductSearchPayload;

// ─── Helpers ─────────────────────────────────────────────────────────────────

/** Idempotency key varsa header objesi döner, yoksa boş obje */
function idempotencyHeader(key?: string): Record<string, string> {
    return key ? { [IDEMPOTENCY_KEY_HEADER]: key } : {};
}

// ─── Product Service ──────────────────────────────────────────────────────────

export const productService = {

    searchProducts: async (body: ProductSearchPayload): Promise<PageResponse<ProductSummary>> => {
        const response = await api.post<PageResponse<ProductSummary>>(
            API_ENDPOINTS.SEARCH.PRODUCTS,
            body,
        );
        return response.data;
    },

    getProductDetail: async (id: number): Promise<ProductDetail> => {
        const response = await api.get<ProductDetail>(API_ENDPOINTS.PRODUCT.BY_ID_PUBLIC(id));
        return response.data;
    },

    // ─── Reviews ─────────────────────────────────────────────────────────────

    getProductReviews: async (
        id: number,
        page = 0,
        size = 10,
    ): Promise<PageResponse<ProductReviewDTO>> => {
        const response = await api.get<PageResponse<ProductReviewDTO>>(
            API_ENDPOINTS.PRODUCT.REVIEWS(id),
            { params: { page, size } },
        );
        return response.data;
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
    ): Promise<PageResponse<TenantProductResponse>> => {
        const response = await api.get<PageResponse<TenantProductResponse>>(
            API_ENDPOINTS.PRODUCT.TENANT_LIST(tenantId),
            { params: { page, size } },
        );
        return response.data;
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

    uploadProductImage: async (file: File): Promise<string> => {
        const formData = new FormData();
        formData.append('file', file);
        const response = await api.post<{ url: string }>(
            '/api/v1/products/images/upload',
            formData,
            // axios multipart header'ı FormData ile otomatik set eder
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

    removeItem: async (productId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.BASKET.REMOVE_ITEM(productId));
    },

    removeFromCart: async (productId: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.BASKET.REMOVE_ITEM(productId));
    },

    updateCartItem: async (_payload: { productId: number; quantity: number }): Promise<void> => {
        console.warn('updateCartItem: Backend endpoint henüz yok');
    },

    clearBasket: async (): Promise<void> => {
        await api.delete(API_ENDPOINTS.BASKET.CLEAR);
    },
};