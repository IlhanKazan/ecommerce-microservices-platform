export const QueryKeys = {
    // Ürün arama (Search Service)
    SEARCH_PRODUCTS: (body: object) =>
        ['searchProducts', body] as const,

    // Ürün detay (Product Service)
    PRODUCT_DETAIL: (id: number) =>
        ['productDetail', id] as const,

    // Yorumlar
    PRODUCT_REVIEWS: (productId: number, page: number, size: number) =>
        ['product-reviews', productId, page, size] as const,

    // Kategoriler
    CATEGORIES: ['categories'] as const,

    // Tenant ürünleri
    TENANT_PRODUCTS: (tenantId: number, page: number, size: number) =>
        ['tenant-products', tenantId, page, size] as const,

    // Sepet
    CART: ['cart'] as const,

    // Depolar
    WAREHOUSES: (tenantId: number) =>
        ['warehouses', tenantId] as const,
};