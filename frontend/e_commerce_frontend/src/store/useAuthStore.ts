import { create } from 'zustand';
import { User as OidcUser } from 'oidc-client-ts';
import type { User as AppUser } from '../types/user';

interface AuthState {
    token: string | null;
    oidcProfile: OidcUser['profile'] | null;
    user: AppUser | null;
    isAuthenticated: boolean;

    setAuth: (token: string, oidcProfile: OidcUser['profile']) => void;
    setUser: (user: AppUser) => void;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: null,
    oidcProfile: null,
    user: null,
    isAuthenticated: false,

    setAuth: (token, oidcProfile) => set({
        token,
        oidcProfile,
        isAuthenticated: true,
    }),

    setUser: (user) => set({ user }),

    clearAuth: () => {
        import('./useCartStore').then(({ useCartStore }) => {
            useCartStore.getState().clearCart();
        });

        set({
            token: null,
            oidcProfile: null,
            user: null,
            isAuthenticated: false,
        });
    },
}));