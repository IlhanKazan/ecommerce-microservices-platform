import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import PageLayout from './components/shared/PageLayout';
import LoadingSpinner from './components/shared/LoadingSpinner';
import { AppRoutes } from './utils/routes';
import { useAuthStore } from "./store/useAuthStore";
import { useAuth } from "react-oidc-context";
import ProtectedRoute from "./components/shared/ProtectedRoute";
import { MerchantProtectedRoute } from "./components/shared/MerchantProtectedRoute";

const HomePage = lazy(() => import('./features/catalog/pages/HomePage.tsx'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const ContactPage = lazy(() => import('./pages/ContactPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));

const AccountLayout = lazy(() => import('./features/user/pages/AccountLayout'));
const AccountOrders = lazy(() => import("./features/user/pages/AccountOrders"));
const AccountProfile = lazy(() => import("./features/user/pages/AccountProfile"));
const AccountAddresses = lazy(() => import("./features/user/pages/AccountAddresses"));

const CartPage = lazy(() => import('./features/checkout/pages/CartPage'));
const CheckoutPage = lazy(() => import('./features/checkout/pages/CheckoutPage'));
const ProductDetailPage = lazy(() => import('./features/catalog/pages/ProductDetailPage.tsx'));
const ProductListPage = lazy(() => import('./features/catalog/pages/ProductListPage.tsx'));

const CreateStorePage = lazy(() => import('./features/tenant/pages/CreateStorePage'));
const SelectStorePage = lazy(() => import('./features/tenant/pages/SelectStorePage'));
const MerchantLayout = lazy(() => import('./features/tenant/layouts/MerchantLayout'));
const MerchantDashboard = lazy(() => import('./features/tenant/pages/MerchantDashboard'));
const MerchantSettings = lazy(() => import('./features/tenant/pages/MerchantSettings'));
const MerchantSubscription = lazy(() => import('./features/tenant/pages/MerchantSubscription'));

const MerchantProducts = lazy(() => import('./features/tenant/pages/MerchantPlaceholderPages').then(module => ({ default: module.MerchantProducts })));
const MerchantOrders = lazy(() => import('./features/tenant/pages/MerchantPlaceholderPages').then(module => ({ default: module.MerchantOrders })));
const MerchantReviews = lazy(() => import('./features/tenant/pages/MerchantPlaceholderPages').then(module => ({ default: module.MerchantReviews })));

function App() {
    const auth = useAuth();
    const setAuth = useAuthStore((state) => state.setAuth);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const fetchMe = useAuthStore((state) => state.fetchMe);

    useEffect(() => {
        if (auth.isAuthenticated && auth.user && auth.user.access_token) {
            setAuth(auth.user.access_token, auth.user.profile);
            fetchMe();
        }
        else if (!auth.isLoading && !auth.isAuthenticated && !auth.user?.access_token) {
            clearAuth();
        }

        if (auth.error) {
            console.error("Token hatası:", auth.error.message);
            clearAuth();
            auth.removeUser().then(() => { window.location.href = "/"; });
        }
    }, [auth.isAuthenticated, auth.user, auth.error, setAuth, clearAuth, fetchMe, auth]);

    if (auth.isLoading) return <LoadingSpinner />;

    return (
        <Suspense fallback={<LoadingSpinner />}>
            <Routes>
                <Route path={AppRoutes.HOME} element={<PageLayout />}>
                    <Route index element={<HomePage />} />
                    <Route path={AppRoutes.ABOUT.slice(1)} element={<AboutPage />} />
                    <Route path={AppRoutes.CONTACT.slice(1)} element={<ContactPage />} />

                    <Route path={AppRoutes.CREATE_STORE.slice(1)} element={<ProtectedRoute><CreateStorePage /></ProtectedRoute>} />

                    <Route path={AppRoutes.ACCOUNT.slice(1)} element={<ProtectedRoute><AccountLayout /></ProtectedRoute>}>
                        <Route index element={<AccountProfile />} />
                        <Route path="orders" element={<AccountOrders />} />
                        <Route path="addresses" element={<AccountAddresses />} />
                    </Route>

                    <Route path={AppRoutes.CART.slice(1)} element={<CartPage />} />
                    <Route path={AppRoutes.CHECKOUT.slice(1)} element={<CheckoutPage />} />
                    <Route path={AppRoutes.PRODUCT_DETAIL.slice(1)} element={<ProductDetailPage />} />
                    <Route path={AppRoutes.PRODUCT_LIST.slice(1)} element={<ProductListPage />} />
                </Route>

                <Route element={<MerchantProtectedRoute />}>
                    <Route path="/merchant/select" element={<SelectStorePage />} />
                    <Route path="/merchant" element={<MerchantLayout />}>
                        <Route index element={<MerchantDashboard />} />
                        <Route path="dashboard" element={<MerchantDashboard />} />
                        <Route path="products" element={<MerchantProducts />} />
                        <Route path="orders" element={<MerchantOrders />} />
                        <Route path="reviews" element={<MerchantReviews />} />
                        <Route path="subscription" element={<MerchantSubscription />} />
                        <Route path="settings" element={<MerchantSettings />} />
                    </Route>
                </Route>

                <Route path="*" element={<NotFoundPage />} />
            </Routes>
        </Suspense>
    );
}

export default App;