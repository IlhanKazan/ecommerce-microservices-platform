import { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { useMe } from '../../query/useUserQueries';
import { useMyTenants } from '../../query/useTenantQueries';
import { useMerchantStore } from '../../store/useMerchantStore';
import LoadingSpinner from './LoadingSpinner';

export const MerchantProtectedRoute = () => {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const { data: user, isLoading: isLoadingUser } = useMe(isAuthenticated);
    const { data: myTenants = [], isLoading: isLoadingTenants } = useMyTenants(isAuthenticated && user?.isMerchant);
    const { activeTenant, setActiveTenant } = useMerchantStore();
    const location = useLocation();

    // Auto-select first tenant if only one exists
    useEffect(() => {
        if (myTenants && myTenants.length === 1 && !activeTenant) {
            setActiveTenant(myTenants[0]);
        }
    }, [myTenants, activeTenant, setActiveTenant]);

    // Loading states - wait for both user and tenant data
    if (!isAuthenticated || isLoadingUser || isLoadingTenants) return <LoadingSpinner />;

    // Not authenticated
    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    // Not a merchant
    if (user && !user.isMerchant && location.pathname !== '/create-store') {
        return <Navigate to="/create-store" replace />;
    }

    // No tenants
    if (!myTenants || myTenants.length === 0) {
        if (location.pathname !== '/create-store' && location.pathname !== '/merchant/select') {
            return <Navigate to="/create-store" replace />;
        }
    }

    // Has tenants but no active tenant selected
    if (myTenants && myTenants.length > 0 && !activeTenant && location.pathname !== '/merchant/select') {
        return <Navigate to="/merchant/select" replace />;
    }

    return <Outlet />;

}