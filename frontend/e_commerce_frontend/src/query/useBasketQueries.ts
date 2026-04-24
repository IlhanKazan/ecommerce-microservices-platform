import { useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { basketService } from '../features/catalog/api/productService';
import { useAuth } from 'react-oidc-context';
import { generateIdempotencyKey, IDEMPOTENCY_KEY_HEADER } from '../utils/idempotencyUtils';
import type { AddItemRequest } from '../types';

export const BASKET_QUERY_KEY = ['basket'] as const;

export const useBasket = () => {
    const auth = useAuth();
    return useQuery({
        queryKey: BASKET_QUERY_KEY,
        queryFn: basketService.getCart,
        enabled: auth.isAuthenticated,
        staleTime: 30_000,
    });
};

export const useBasketItemCount = (): number => {
    const { data } = useBasket();
    if (!data) return 0;
    return data.items.reduce((total, item) => total + item.quantity, 0);
};

/**
 * Sepete ürün ekleme — idempotency key ile.
 *
 * Kullanıcı "Sepete Ekle"ye çift tıklarsa veya ağ hatası sonrası
 * retry atılırsa backend aynı key'i görüp ikinci işlemi reddeder.
 *
 * Key lifecycle:
 *   - Hook mount'ta üretilir
 *   - onSuccess'te yenilenir (bir sonraki ürün için farklı key)
 *   - onError'da aynı kalır → retry'da backend dedupe yapabilir
 */
export const useAddToBasket = () => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (newItem: AddItemRequest) =>
            basketService.addToCart(newItem, idempotencyKey.current),

        // Optimistic update
        onMutate: async (newItem: AddItemRequest) => {
            await queryClient.cancelQueries({ queryKey: BASKET_QUERY_KEY });
            const previousBasket = queryClient.getQueryData(BASKET_QUERY_KEY);

            queryClient.setQueryData(BASKET_QUERY_KEY, (old: any) => {
                if (!old) return old;
                const existingItem = old.items.find((i: any) => i.productId === newItem.productId);
                const updatedItems = existingItem
                    ? old.items.map((i: any) =>
                        i.productId === newItem.productId
                            ? { ...i, quantity: i.quantity + newItem.quantity }
                            : i,
                    )
                    : [
                        ...old.items,
                        {
                            productId:   newItem.productId,
                            productName: '...',
                            quantity:    newItem.quantity,
                            price:       0,
                            imageUrl:    null,
                        },
                    ];
                return { ...old, items: updatedItems };
            });

            return { previousBasket };
        },

        onSuccess: () => {
            // Başarılı → bir sonraki "Sepete Ekle" için yeni key
            idempotencyKey.current = generateIdempotencyKey();
        },

        onError: (_err, _vars, context) => {
            // Rollback — key değişmez, retry aynı key ile gidebilir
            if (context?.previousBasket) {
                queryClient.setQueryData(BASKET_QUERY_KEY, context.previousBasket);
            }
        },

        onSettled: () => {
            queryClient.invalidateQueries({ queryKey: BASKET_QUERY_KEY });
        },

        retry: 1,
    });
};

export const useRemoveFromBasket = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: basketService.removeItem,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: BASKET_QUERY_KEY });
        },
    });
};

export const useClearBasket = () => {
    const queryClient = useQueryClient();
    return useMutation({
        mutationFn: basketService.clearBasket,
        onSuccess: () => {
            queryClient.setQueryData(BASKET_QUERY_KEY, null);
        },
    });
};