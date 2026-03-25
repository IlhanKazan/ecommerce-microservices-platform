import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { productService, basketService, type SearchPayload } from '../features/catalog/api/productService.ts';
import { tenantService } from '../features/tenant/api/tenantService';

export const useSearchProducts = (body: SearchPayload) => {
    return useQuery({
        queryKey: ['searchProducts', body],
        queryFn: () => productService.searchProducts(body),
        staleTime: 1000 * 60 * 2,
    });
};

export const useGetProductDetail = (productId: number) => {
    return useQuery({
        queryKey: ['productDetail', productId],
        queryFn: () => productService.getProductDetail(productId),
        enabled: !!productId,
    });
};

export const useGetWarehouses = (tenantId: number) => {
    return useQuery({
        queryKey: ['warehouses', tenantId],
        queryFn: () => tenantService.getWarehouses(tenantId),
        enabled: !!tenantId,
    });
};

export const useCreateWarehouse = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (payload: { code: string; name: string; locationDetails: string }) =>
            tenantService.createWarehouse(tenantId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['warehouses', tenantId] });
        },
    });
};

export const useAddManualStock = (tenantId: number) => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: (payload: { warehouseId: number; productId: number; amount: number }) =>
            tenantService.addManualStock(tenantId, payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['warehouses', tenantId] });
            queryClient.invalidateQueries({ queryKey: ['searchProducts'] });
            queryClient.invalidateQueries({ queryKey: ['productDetail'] });
        },
    });
};

export const useAddToCart = () => {
    return useMutation({ mutationFn: basketService.addToCart });
};

export const useUpdateCartItem = () => {
    return useMutation({ mutationFn: basketService.updateCartItem });
};

export const useRemoveFromCart = () => {
    return useMutation({ mutationFn: basketService.removeFromCart });
};