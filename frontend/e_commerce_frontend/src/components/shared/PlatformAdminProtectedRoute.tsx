import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../store/useAuthStore';
import { useMe } from '../../query/useUserQueries';
import LoadingSpinner from './LoadingSpinner';

/**
 * Sadece platform-admin rolüne sahip kullanıcıların /admin altına girmesine izin verir.
 * Rol bilgisi backend /users/me yanıtındaki isPlatformAdmin alanından gelir
 * (JWT'deki platform-admin client rolünden türetilir).
 */
export const PlatformAdminProtectedRoute = () => {
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
    const { data: user, isLoading } = useMe(isAuthenticated);

    if (!isAuthenticated) {
        return <Navigate to="/" replace />;
    }

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!user?.isPlatformAdmin) {
        return <Navigate to="/" replace />;
    }

    return <Outlet />;
};

export default PlatformAdminProtectedRoute;
