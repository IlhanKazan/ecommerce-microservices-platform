import { api } from '../../../lib/axios';
import { API_ENDPOINTS } from '../../../config/apiEndpoints';

export interface SearchPayload {
    keyword?: string;
    categoryId?: number;
    minPrice?: number;
    maxPrice?: number;
    brands?: string[];
    page: number;
    size: number;
}

export const productService = {
    searchProducts: async (body: SearchPayload) => {
        const response = await api.post(API_ENDPOINTS.SEARCH.PUBLIC, body);
        return response.data;
    },

    getProductDetail: async (id: number) => {
        const response = await api.get(API_ENDPOINTS.PRODUCT.BY_ID_PUBLIC(id));
        return response.data;
    }
};

export const basketService = {
    addToCart: async (payload: { productId: number; quantity: number }) => {
        const response = await api.post(API_ENDPOINTS.BASKET.ADD, payload);
        return response.data;
    },
    updateCartItem: async (payload: { productId: number; quantity: number }) => {
        const response = await api.put(API_ENDPOINTS.BASKET.UPDATE, payload);
        return response.data;
    },
    removeFromCart: async (productId: number) => {
        const response = await api.delete(API_ENDPOINTS.BASKET.REMOVE(productId));
        return response.data;
    }
};