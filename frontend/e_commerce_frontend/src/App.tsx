import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import PageLayout from './components/shared/PageLayout';
import LoadingSpinner from './components/shared/LoadingSpinner';
import { NotificationProvider } from './components/shared/NotificationProvider';
import { AppRoutes } from './utils/routes';
import { useAuthStore } from "./store/useAuthStore";
import { useCartStore } from "./store/useCartStore";
import { useMe } from './query/useUserQueries';
import { useAuth } from "react-oidc-context";
import ProtectedRoute from "./components/shared/ProtectedRoute";
import { MerchantProtectedRoute } from "./components/shared/MerchantProtectedRoute";
import ToastContainer from "./components/shared/ToastContainer.tsx";
import { useAddToBasket } from './query/useBasketQueries';
import { useGetCategories } from './query/useProductQueries';
import { useCategoryStore } from './store/useCategoryStore';

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
const MerchantWarehouse = lazy(() => import('./features/tenant/pages/MerchantWarehousePage'));
const MerchantReviewsPage = lazy(() => import('./features/tenant/pages/MerchantReviewsPage'));

function App() {
    const auth = useAuth();
    const setAuth = useAuthStore((state) => state.setAuth);
    const clearAuth = useAuthStore((state) => state.clearAuth);
    const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

    const setUser = useAuthStore((state) => state.setUser);
    const { data: userData } = useMe(isAuthenticated);
    const { items: localCartItems, clearCart } = useCartStore();
    const { mutate: addToBasket } = useAddToBasket();

    useEffect(() => {
        if (userData) setUser(userData);
    }, [userData, setUser]);

    useEffect(() => {
        if (auth.isAuthenticated && auth.user?.access_token) {
            setAuth(auth.user.access_token, auth.user.profile);

            // GUEST CART SENKRONİZASYONU
            if (localCartItems.length > 0) {
                // Localdeki her ürünü backend'e at
                localCartItems.forEach(item => {
                    addToBasket({ productId: item.productId, quantity: item.quantity });
                });
                clearCart(); // Backend'e geçince local'i sıfırla
            }
        } else if (!auth.isLoading && !auth.isAuthenticated) {
            clearAuth();
        }

        if (auth.error) {
            console.error('Token hatası:', auth.error.message);
            clearAuth();
            auth.removeUser().then(() => { window.location.href = '/'; });
        }
    }, [auth.isAuthenticated, auth.user, auth.error, setAuth, clearAuth]);

    const { data: categoryData } = useGetCategories();
    const setCategories = useCategoryStore((state) => state.setCategories);

    useEffect(() => {
        if (categoryData) {
            setCategories(categoryData);
        }
    }, [categoryData, setCategories]);

    if (auth.isLoading) return <LoadingSpinner />;

    return (
        <NotificationProvider>
            <ToastContainer />
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
                    <Route
                        path={AppRoutes.CHECKOUT.slice(1)}
                        element={
                            <ProtectedRoute>
                                <CheckoutPage />
                            </ProtectedRoute>
                        }
                    />
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
                        <Route path="reviews"    element={<MerchantReviewsPage />} />
                        <Route path="warehouses" element={<MerchantWarehouse />} />
                    </Route>
                </Route>

                <Route path="*" element={<NotFoundPage />} />
            </Routes>
        </Suspense>
        </NotificationProvider>
    );
}

export default App;