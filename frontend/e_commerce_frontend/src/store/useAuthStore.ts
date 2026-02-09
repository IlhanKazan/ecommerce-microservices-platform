import { create } from 'zustand';
import { User as OidcUser } from 'oidc-client-ts';
import type { User as AppUser } from '../types/user';
import { userService } from '../service/userService';

interface AuthState {
    token: string | null;
    oidcProfile: OidcUser['profile'] | null;

    user: AppUser | null;

    isAuthenticated: boolean;
    isLoadingUser: boolean;

    setAuth: (token: string, oidcProfile: OidcUser['profile']) => void;
    fetchMe: () => Promise<void>;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: null,
    oidcProfile: null,
    user: null,
    isAuthenticated: false,
    isLoadingUser: false,

    setAuth: (token, oidcProfile) => set({
        token,
        oidcProfile,
        isAuthenticated: true
    }),

    fetchMe: async () => {
        set({ isLoadingUser: true });
        try {
            const userData = await userService.getMe();
            set({ user: userData });
        } catch (error) {
            console.error("Kullanıcı detayları çekilemedi:", error);
        } finally {
            set({ isLoadingUser: false });
        }
    },

    clearAuth: () => set({
        token: null,
        oidcProfile: null,
        user: null,
        isAuthenticated: false,
        isLoadingUser: false
    }),
}));