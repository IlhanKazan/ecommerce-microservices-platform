import {type PropsWithChildren, useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import LoadingSpinner from './LoadingSpinner';

const ProtectedRoute = ({ children }: PropsWithChildren) => {
    const auth = useAuth();

    useEffect(() => {
        if (!auth.isLoading && !auth.isAuthenticated) {
            auth.signinRedirect();
        }
    }, [auth.isLoading, auth.isAuthenticated, auth]);

    if (auth.isLoading) {
        return <LoadingSpinner />;
    }

    if (!auth.isAuthenticated) {
        return <LoadingSpinner />;
    }

    return <>{children}</>;
};

export default ProtectedRoute;