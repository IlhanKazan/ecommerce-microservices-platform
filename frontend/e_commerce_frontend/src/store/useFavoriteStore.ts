import { create } from 'zustand';
import { productService } from '../features/catalog/api/productService';
import { useToastStore } from './useToastStore';

interface FavoriteState {
    ids: Set<number>;
    loaded: boolean;
    loadIds: () => Promise<void>;
    toggle: (productId: number) => Promise<void>;
    clear: () => void;
}

/**
 * Favori ürün id'lerini tutar. Kart/detayda kalp state'i buradan okunur.
 * toggle optimistic: store hemen güncellenir, backend hata verirse geri alınır.
 */
export const useFavoriteStore = create<FavoriteState>((set, get) => ({
    ids: new Set<number>(),
    loaded: false,

    loadIds: async () => {
        try {
            const ids = await productService.getFavoriteIds();
            set({ ids: new Set(ids), loaded: true });
        } catch {
            set({ loaded: true });
        }
    },

    toggle: async (productId) => {
        const current = get().ids;
        const isFav = current.has(productId);

        const optimistic = new Set(current);
        if (isFav) optimistic.delete(productId);
        else optimistic.add(productId);
        set({ ids: optimistic });

        try {
            if (isFav) await productService.removeFavorite(productId);
            else await productService.addFavorite(productId);
        } catch {
            set({ ids: new Set(current) }); // geri al
            useToastStore.getState().error('Favori güncellenemedi.');
        }
    },

    clear: () => set({ ids: new Set<number>(), loaded: false }),
}));
