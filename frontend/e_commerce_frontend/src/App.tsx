import { lazy, Suspense, useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import PageLayout from './components/shared/PageLayout';
import LoadingSpinner from './components/shared/LoadingSpinner';
import { NotificationProvider } from './components/shared/NotificationProvider';
import { AppRoutes } from './utils/routes';
import { useAuthStore } from "./store/useAuthStore";
import { useCartStore } from "./store/useCartStore";
import { useFavoriteStore } from "./store/useFavoriteStore";
import { useMe } from './query/useUserQueries';
import { useAuth } from "react-oidc-context";
import ProtectedRoute from "./components/shared/ProtectedRoute";
import { MerchantProtectedRoute } from "./components/shared/MerchantProtectedRoute";
import ToastContainer from "./components/shared/ToastContainer.tsx";
import { basketService } from './features/catalog/api/productService';
import { useGetCategories } from './query/useProductQueries';
import { useCategoryStore } from './store/useCategoryStore';
import { BASKET_QUERY_KEY } from './query/useBasketQueries';

const HomePage = lazy(() => import('./features/catalog/pages/HomePage.tsx'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const ContactPage = lazy(() => import('./pages/ContactPage'));
const NotFoundPage = lazy(() => import('./pages/NotFoundPage'));

const AccountLayout = lazy(() => import('./features/user/pages/AccountLayout'));
const AccountOrders = lazy(() => import("./features/user/pages/AccountOrders"));
const AccountProfile = lazy(() => import("./features/user/pages/AccountProfile"));
const AccountAddresses = lazy(() => import("./features/user/pages/AccountAddresses"));
const AccountFavorites = lazy(() => import("./features/user/pages/AccountFavorites"));

const CartPage = lazy(() => import('./features/checkout/pages/CartPage'));
const CheckoutPage = lazy(() => import('./features/checkout/pages/CheckoutPage'));
const ProductDetailPage = lazy(() => import('./features/catalog/pages/ProductDetailPage.tsx'));
const ProductListPage = lazy(() => import('./features/catalog/pages/ProductListPage.tsx'));
const StorePage = lazy(() => import('./features/catalog/pages/StorePage.tsx'));

const CreateStorePage = lazy(() => import('./features/tenant/pages/CreateStorePage'));
const SelectStorePage = lazy(() => import('./features/tenant/pages/SelectStorePage'));
const MerchantLayout = lazy(() => import('./features/tenant/layouts/MerchantLayout'));
const MerchantDashboard = lazy(() => import('./features/tenant/pages/MerchantDashboard'));
const MerchantSettings = lazy(() => import('./features/tenant/pages/MerchantSettings'));
const MerchantSubscription = lazy(() => import('./features/tenant/pages/MerchantSubscription'));

const MerchantProducts = lazy(() => import('./features/tenant/pages/MerchantPlaceholderPages').then(module => ({ default: module.MerchantProducts })));
const MerchantOrders = lazy(() => import('./features/tenant/pages/MerchantOrdersPage'));
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
    // clearCart — guest sepeti merge sonrası temizliği için. Sepet item'larını
    // burada selector'la dinlemiyoruz: değişimde App ağacını gereksiz re-render eder
    // ve login/merge effect'ini tetiklerdi. Anlık ihtiyaçta getState() kullanılır.
    const clearCart = useCartStore((s) => s.clearCart);
    const queryClient = useQueryClient();

    useEffect(() => {
        if (userData) setUser(userData);
    }, [userData, setUser]);

    useEffect(() => {
        if (auth.isAuthenticated && auth.user?.access_token) {
            setAuth(auth.user.access_token, auth.user.profile);

            // GUEST CART → HESAP SEPETİ: tek atomik merge çağrısı (miktarlar toplanır).
            // Başarıda local sepet temizlenir; HATA olursa local KORUNUR (veri kaybı yok).
            // Not: guest sepetini deps'ten okumuyoruz (yoksa her "sepete ekle" bu
            // effect'i tetikler); anlık snapshot'ı getState() ile alıyoruz.
            const guestItems = useCartStore.getState().items;
            if (guestItems.length > 0) {
                basketService.mergeCart(
                    guestItems.map(item => ({ productId: item.productId, quantity: item.quantity }))
                )
                    .then(() => {
                        clearCart();
                        queryClient.invalidateQueries({ queryKey: BASKET_QUERY_KEY });
                    })
                    .catch((err) => {
                        console.error('Sepet birleştirme başarısız — local sepet korunuyor:', err);
                    });
            }
        } else if (!auth.isLoading && !auth.isAuthenticated) {
            // Sadece GERÇEK logout'ta temizle. Hiç giriş yapmamış guest için
            // clearAuth çağırma — clearAuth() guest sepetini de siler (clearCart).
            if (useAuthStore.getState().isAuthenticated) {
                clearAuth();
            }
        }

        if (auth.error) {
            console.error('Token hatası:', auth.error.message);
            clearAuth();
            auth.removeUser().then(() => { window.location.href = '/'; });
        }
    }, [auth, clearCart, queryClient, setAuth, clearAuth]);

    const { data: categoryData } = useGetCategories();
    const setCategories = useCategoryStore((state) => state.setCategories);

    useEffect(() => {
        if (categoryData) {
            setCategories(categoryData);
        }
    }, [categoryData, setCategories]);

    // Favori id'lerini girişte yükle, çıkışta temizle
    useEffect(() => {
        if (isAuthenticated) {
            useFavoriteStore.getState().loadIds();
        } else {
            useFavoriteStore.getState().clear();
        }
    }, [isAuthenticated]);

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
                        <Route path="favorites" element={<AccountFavorites />} />
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
                    <Route path={AppRoutes.STORE.slice(1)} element={<StorePage />} />
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