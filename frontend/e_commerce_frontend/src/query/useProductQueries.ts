import { useQuery } from '@tanstack/react-query';
import type { Product, PaginatedResult } from '../types';
import { QueryKeys } from './queryKeys';
import { MOCK_PRODUCTS } from '../data/mockProducts';

export const useProducts = (page: number, pageSize: number, category?: string) => {

    const fetcher = async (): Promise<PaginatedResult<Product>> => {
        await new Promise(resolve => setTimeout(resolve, 800));

        const filtered = category ? MOCK_PRODUCTS.filter(p => p.category === category) : MOCK_PRODUCTS;

        const start = (page - 1) * pageSize;
        const end = start + pageSize;
        const data = filtered.slice(start, end);

        return {
            data,
            totalElements: filtered.length,
            totalPages: Math.ceil(filtered.length / pageSize),
            page,
            pageSize,
        };
    };

    const queryKey = QueryKeys.PRODUCTS(page, { pageSize, category });

    return useQuery({
        queryKey,
        queryFn: fetcher,
        staleTime: 1000 * 60 * 5,
    });
};

export const useProductDetail = (productId: number) => {
    const fetcher = async (): Promise<Product | null> => {
        await new Promise(resolve => setTimeout(resolve, 500));
        return MOCK_PRODUCTS.find(p => p.id === productId) || null;
    };

    const queryKey = QueryKeys.PRODUCT_DETAIL(productId);

    return useQuery({
        queryKey,
        queryFn: fetcher,
        staleTime: 1000 * 60 * 60,
        enabled: productId > 0,
    });
};