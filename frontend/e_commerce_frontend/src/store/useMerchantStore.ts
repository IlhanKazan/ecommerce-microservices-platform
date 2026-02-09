import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { tenantService } from '../service/tenantService';
import type { TenantSummary } from '../types/tenant';

interface MerchantState {
    myTenants: TenantSummary[];
    activeTenant: TenantSummary | null;
    isLoading: boolean;

    fetchMyTenants: () => Promise<void>;
    setActiveTenant: (tenant: TenantSummary) => void;
    clearMerchantSession: () => void;
}

export const useMerchantStore = create<MerchantState>()(
    persist(
        (set) => ({
            myTenants: [],
            activeTenant: null,
            isLoading: false,

            fetchMyTenants: async () => {
                set({ isLoading: true });
                try {
                    const tenants = await tenantService.getMyTenants();
                    set({ myTenants: tenants });

                    if (tenants.length === 1) {
                        set({ activeTenant: tenants[0] });
                    }
                } catch (error) {
                    console.error("Mağazalar çekilemedi", error);
                    set({ myTenants: [] });
                } finally {
                    set({ isLoading: false });
                }
            },

            setActiveTenant: (tenant) => set({ activeTenant: tenant }),
            clearMerchantSession: () => set({ activeTenant: null, myTenants: [] })
        }),
        {
            name: 'merchant-storage',
            partialize: (state) => ({ activeTenant: state.activeTenant }),
        }
    )
);