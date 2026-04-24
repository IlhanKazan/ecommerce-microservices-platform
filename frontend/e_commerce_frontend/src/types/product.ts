// ─── Search Service ──────────────────────────────────────────────────────────

/** Search Service'ten gelen liste öğesi */
export interface ProductSummary {
    id: string;
    tenantId: number;
    categoryId: number | null;
    categoryName: string | null;
    sku: string;
    name: string;
    brand: string | null;
    price: number;
    discountedPrice: number | null;
    currency: string;
    mainImageUrl: string | null;
    ratingAverage: number | null;
    reviewCount: number;
    salesStatus: string;
    inStock: boolean;
}

// ─── Product Service ─────────────────────────────────────────────────────────

/** GET /api/v1/public/products/{id} */
export interface ProductDetail {
    id: number;
    tenantId: number;
    categoryId: number | null;
    categoryName: string | null;
    parentProductId: number | null;
    name: string;
    description: string;
    sku: string;
    brand: string | null;
    price: number;
    discountedPrice: number | null;
    currency: string;
    mainImageUrl: string | null;
    imageUrls: string[];
    attributes: Record<string, string>;
    ratingAverage: number | null;
    reviewCount: number;
    minOrderQty: number;
    maxOrderQty: number | null;
    status: string;
    salesStatus: string;
}

// ─── Review ──────────────────────────────────────────────────────────────────

/** GET /api/v1/public/products/{id}/reviews */
export interface ProductReviewDTO {
    id: number;
    /** UUID — backend maskeli göstermiyor, avatarda baş harf kullan */
    userId: string;
    title: string;
    reviewText: string;
    rating: number;
    sentimentLabel: string | null;
    isVerifiedPurchase: boolean;
    helpfulCount: number;
    notHelpfulCount: number;
    imageUrls: string[] | null;
    sellerResponse: string | null;
    sellerResponseAt: string | null;
    reviewedAt: string;
    status: string;
}

export interface ReviewCreateRequest {
    title: string;
    reviewText: string;
    rating: number;
    imageUrls?: string[];
}

// ─── Category ────────────────────────────────────────────────────────────────

/** GET /api/v1/categories — ağaç yapısı */
export interface CategoryResponse {
    id: number;
    name: string;
    slug: string;
    description: string | null;
    imageUrl: string | null;
    icon: string | null;
    level: number | null;
    fullPath: string | null;
    subCategories: CategoryResponse[];
}

// ─── Search Payload ───────────────────────────────────────────────────────────

export interface ProductSearchPayload {
    keyword?: string;
    /** Seçilen kategori + tüm torun kategori ID'leri */
    categoryIds?: number[];
    brands?: string[];
    minPrice?: number;
    maxPrice?: number;
    inStock?: boolean;
    sortBy?: 'newest' | 'price_asc' | 'price_desc' | 'popular' | 'rating';
    page: number;
    size: number;
}

// ─── Pagination ───────────────────────────────────────────────────────────────

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    pageSize: number;
    pageNumber: number;
    isLast: boolean;
}

// ─── Tenant Product Management ────────────────────────────────────────────────

export interface TenantProductResponse {
    id: number;
    tenantId: number;
    categoryId: number;
    categoryName: string;
    parentProductId: number | null;
    name: string;
    description: string;
    sku: string;
    brand: string | null;
    price: number;
    discountedPrice: number | null;
    currency: string;
    mainImageUrl: string | null;
    imageUrls: string[];
    attributes: Record<string, string>;
    ratingAverage: number | null;
    reviewCount: number;
    status: string;
    salesStatus: string;
}

export interface ProductCreateRequest {
    categoryId: number;
    parentProductId?: number;
    name: string;
    description?: string;
    sku: string;
    brand?: string;
    price: number;
    currency?: string;
    weightGrams?: number;
    dimensionsCm?: string;
    mainImageUrl?: string;
    imageUrls?: string[];
    attributes?: Record<string, string>;
    minOrderQty?: number;
    maxOrderQty?: number;
    tags?: string[];
    seoTitle?: string;
    seoDescription?: string;
    seoKeywords?: string;
}

export interface ProductUpdateRequest extends ProductCreateRequest {
    // PUT body aynı alanları taşıyor
}