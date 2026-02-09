export const QueryKeys = {
    PRODUCTS: (page: number, filters: { pageSize: number, category?: string }) =>
        ['products', { page, ...filters }] as const,

    PRODUCT_DETAIL: (id: number) =>
        ['product', id] as const,

    CART: ['cart'] as const,
};