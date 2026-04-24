import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface CartItem {
    productId: number;
    name: string;
    price: number;
    quantity: number;
    mainImageUrl: string | null | undefined;
}

interface CartState {
    items: CartItem[];
    addItem: (item: CartItem) => void;
    removeItem: (productId: number) => void;
    updateQuantity: (productId: number, quantity: number) => void;
    clearCart: () => void;
    getTotalPrice: () => number;
    getItemCount: () => number;
}

export const useCartStore = create<CartState>()(
    persist(
        (set, get) => ({
            items: [],

            addItem: (newItem) => set((state) => {
                const existing = state.items.find(i => i.productId === newItem.productId);
                if (existing) {
                    return {
                        items: state.items.map(i =>
                            i.productId === newItem.productId
                                ? { ...i, quantity: i.quantity + newItem.quantity }
                                : i
                        ),
                    };
                }
                return { items: [...state.items, newItem] };
            }),

            removeItem: (productId) => set((state) => ({
                items: state.items.filter(i => i.productId !== productId),
            })),

            updateQuantity: (productId, quantity) => set((state) => ({
                items: state.items.map(i =>
                    i.productId === productId ? { ...i, quantity } : i
                ),
            })),

            clearCart: () => set({ items: [] }),

            getTotalPrice: () =>
                get().items.reduce((sum, i) => sum + i.price * i.quantity, 0),

            getItemCount: () =>
                get().items.reduce((sum, i) => sum + i.quantity, 0),
        }),
        {
            name: 'guest-cart',
        }
    )
);