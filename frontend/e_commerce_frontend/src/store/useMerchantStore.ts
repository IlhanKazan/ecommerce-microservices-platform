import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { TenantSummary } from '../types/tenant';

interface MerchantState {
    // Tenant list is now managed by useMyTenants() TanStack Query hook
    // This store only manages UI state for the active tenant selection
    
    activeTenant: TenantSummary | null;

    setActiveTenant: (tenant: TenantSummary) => void;
    clearMerchantSession: () => void;
}

export const useMerchantStore = create<MerchantState>()(
    persist(
        (set) => ({
            activeTenant: null,

            setActiveTenant: (tenant) => set({ activeTenant: tenant }),
            clearMerchantSession: () => set({ activeTenant: null })
        }),
        {
            name: 'merchant-storage',
            partialize: (state) => ({ activeTenant: state.activeTenant }),
        }
    )
);