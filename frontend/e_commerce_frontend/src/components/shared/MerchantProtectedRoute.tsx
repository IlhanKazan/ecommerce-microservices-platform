import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { useMerchantStore } from '../../store/useMerchantStore';
import LoadingSpinner from './LoadingSpinner';

export const MerchantProtectedRoute = () => {
    const { isAuthenticated, user } = useAuthStore();
    const { activeTenant, myTenants, fetchMyTenants, isLoading: isStoreLoading } = useMerchantStore();
    const location = useLocation();

    const [isVerifying, setIsVerifying] = useState(true);

    useEffect(() => {
        const verifyMerchantStatus = async () => {
            if (isAuthenticated && user) {
                if (user.isMerchant && myTenants.length === 0) {
                    await fetchMyTenants();
                }
            }
            setIsVerifying(false);
        };

        verifyMerchantStatus();
    }, [isAuthenticated, user?.isMerchant]);

    if (isStoreLoading || isVerifying) return <LoadingSpinner />;

    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    if (user && !user.isMerchant && location.pathname !== '/create-store') {
        return <Navigate to="/create-store" replace />;
    }

    if (myTenants.length === 0 && location.pathname !== '/create-store') {
        return <Navigate to="/create-store" replace />;
    }

    if (myTenants.length > 0 && !activeTenant && location.pathname !== '/merchant/select') {
        return <Navigate to="/merchant/select" replace />;
    }

    return <Outlet />;
};