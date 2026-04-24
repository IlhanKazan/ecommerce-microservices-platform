import { useQuery, useQueryClient } from '@tanstack/react-query';
import { tenantService } from '../features/tenant/api/tenantService';

export const TENANT_QUERY_KEYS = {
    myTenants: ['tenant', 'myTenants'] as const,
    all: ['tenant'] as const,
};

/**
 * Fetch list of tenant/stores for the current authenticated user
 * Only runs if enabled (caller must check isAuthenticated && isMerchant)
 * Uses TanStack Query for caching and auto-refetching
 */
export const useMyTenants = (enabled: boolean = true) => {
    return useQuery({
        queryKey: TENANT_QUERY_KEYS.myTenants,
        queryFn: () => tenantService.getMyTenants(),
        enabled: enabled,
        staleTime: 1000 * 60 * 10, // 10 minutes
        retry: 1,
    });
};

/**
 * Invalidate tenant list cache when needed (after creating new tenant, etc)
 */
export const useInvalidateMyTenants = () => {
    const queryClient = useQueryClient();
    return () => {
        queryClient.invalidateQueries({ queryKey: TENANT_QUERY_KEYS.myTenants });
    };
};
