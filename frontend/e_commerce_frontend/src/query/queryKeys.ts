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
    TENANT_PRODUCTS: (tenantId: number, page: number, size: number, q = '', salesStatus = '', sort = '') =>
        ['tenant-products', tenantId, page, size, q, salesStatus, sort] as const,

    // Sepet
    CART: ['cart'] as const,

    // Depolar
    WAREHOUSES: (tenantId: number) =>
        ['warehouses', tenantId] as const,

    // Tenant stok özeti
    TENANT_STOCKS: (tenantId: number) =>
        ['tenant-stocks', tenantId] as const,

    // Sipariş key'leri
    MY_ORDERS: (page: number, size: number) =>
        ['my-orders', page, size] as const,

    ORDER_DETAIL: (orderId: number) =>
        ['order-detail', orderId] as const,

    TENANT_ORDERS: (tenantId: number, page: number, size: number, status = '', q = '') =>
        ['tenant-orders', tenantId, page, size, status, q] as const,
};