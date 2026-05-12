---
name: frontend-component-patterns
description: Use when working with shared frontend components — MUI theme customization, ImageUploadField (single/multi), ProtectedRoute, MerchantProtectedRoute, NotificationProvider, ToastContainer, axios interceptor setup, lazy-loaded routes, or form modals. Covers reusable patterns and infrastructure.
---

# Frontend Component & Infrastructure Patterns

Bu skill IlhanKazan frontend'inin shared component'leri ve infrastructure pattern'leri için referans. Mevcut kod tabanından türetildi.

## MUI Theme

`utils/customTheme.ts`:

```tsx
import { createTheme } from '@mui/material/styles';

const customTheme = createTheme({
    palette: {
        primary: {
            main: '#7D5525',          // Brown
            light: '#C8A882',
            dark: '#5C3E1A',
            contrastText: '#FFFFFF',
        },
        secondary: {
            main: '#1D466E',          // Navy blue
            light: '#4A7BA7',
            dark: '#132E48',
        },
        background: {
            default: '#F5F5F5',
            paper: '#FFFFFF',
        },
        text: {
            primary: '#212121',
            secondary: '#757575',
        },
    },
    typography: {
        fontFamily: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', /* ... */].join(','),
        h1: { fontSize: '2.5rem', fontWeight: 700 },
        button: { textTransform: 'none' },
    },
    components: {
        MuiButton: {
            defaultProps: { disableElevation: true },
            styleOverrides: {
                root: { borderRadius: 8 },
            },
        },
    },
});
```

**Kural:**
- Renk değişikliği tüm UI'yi etkiler — kullanıcıya danış
- Buton borderRadius default 8 (yuvarlak köşe), button textTransform `none` (CAPS yok)
- Yeni component eklerken `theme.palette.primary.main` gibi tema değişkenlerini kullan, hard-code renk yazma

```tsx
// ❌ Hard-code
sx={{ color: '#7D5525' }}

// ✅ Tema referansı
sx={{ color: 'primary.main' }}
```

## Provider hiyerarşisi (`main.tsx`)

```tsx
ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <ThemeProvider theme={theme}>
            <AuthProvider {...oidcConfig}>
                <BrowserRouter>
                    <QueryClientProvider client={queryClient}>
                        <CssBaseline />
                        {GlobalScrollbarFix}
                        <App />
                    </QueryClientProvider>
                </BrowserRouter>
            </AuthProvider>
        </ThemeProvider>
    </React.StrictMode>,
);
```

**Sıra kritik (dıştan içe):**
1. `ThemeProvider` — tüm component'ler theme'e erişir
2. `AuthProvider` (react-oidc-context) — auth state global
3. `BrowserRouter` — routing
4. `QueryClientProvider` — React Query
5. `CssBaseline` — MUI reset
6. `GlobalScrollbarFix` — body scroll fix
7. `App` — routes

**QueryClient default'lar:**
```tsx
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 60_000,
            retry: 1,
            refetchOnWindowFocus: false,
        },
    },
});
```

**OIDC config:**
```tsx
const oidcConfig: AuthProviderProps = {
    authority: `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}`,
    client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
    redirect_uri: window.location.origin,
    automaticSilentRenew: true,
    loadUserInfo: true,
    accessTokenExpiringNotificationTimeInSeconds: 60,
    onSigninCallback: (_user) => {
        window.history.replaceState({}, document.title, window.location.pathname);
    }
};
```

## Lazy loading + Suspense

`App.tsx`:

```tsx
import { lazy, Suspense } from 'react';

const HomePage = lazy(() => import('./features/catalog/pages/HomePage.tsx'));
const ProductDetailPage = lazy(() => import('./features/catalog/pages/ProductDetailPage.tsx'));

// Named export'tan lazy:
const MerchantProducts = lazy(() => import('./features/tenant/pages/MerchantPlaceholderPages')
    .then(module => ({ default: module.MerchantProducts })));

function App() {
    return (
        <Suspense fallback={<LoadingSpinner />}>
            <Routes>
                <Route path="/" element={<HomePage />} />
                ...
            </Routes>
        </Suspense>
    );
}
```

**Kural:** Sayfa-level component'ler `lazy()` ile import edilir. Tek `<Suspense>` `App.tsx`'in en üstünde. Loading fallback `LoadingSpinner`.

## Routing

`utils/routes.ts`:

```tsx
export const AppRoutes = {
    HOME: '/',
    PRODUCT_LIST: '/productlist',
    PRODUCT_DETAIL: '/product/:productId',
    CART: '/cart',
    CHECKOUT: '/checkout',
    ABOUT: '/about',
    CONTACT: '/contact',
    CREATE_STORE: '/create-store',
    ACCOUNT: '/user',
    ORDER_HISTORY: '/user/orders',
    MERCHANT: '/merchant',
    MERCHANT_DASHBOARD: '/merchant/dashboard',
    MERCHANT_SELECT: '/merchant/select',
};
```

**Nested route'larda `slice(1)`:**

```tsx
<Route path={AppRoutes.HOME} element={<PageLayout />}>
    <Route index element={<HomePage />} />
    {/* Nested route relative path bekler — leading slash çıkar */}
    <Route path={AppRoutes.ABOUT.slice(1)} element={<AboutPage />} />
    <Route path={AppRoutes.CART.slice(1)} element={<CartPage />} />
</Route>
```

`AppRoutes.ABOUT = '/about'` ama nested'da `'about'` lazım — `slice(1)` ile leading slash çıkar.

## ProtectedRoute (auth gerekli)

`components/shared/ProtectedRoute.tsx`:

```tsx
import { type PropsWithChildren, useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import LoadingSpinner from './LoadingSpinner';

const ProtectedRoute = ({ children }: PropsWithChildren) => {
    const auth = useAuth();

    useEffect(() => {
        if (!auth.isLoading && !auth.isAuthenticated) {
            auth.signinRedirect();
        }
    }, [auth.isLoading, auth.isAuthenticated, auth]);

    if (auth.isLoading) return <LoadingSpinner />;
    if (!auth.isAuthenticated) return <LoadingSpinner />;

    return <>{children}</>;
};
```

**Kullanım:** Children sarmalama:
```tsx
<Route path="/checkout" element={
    <ProtectedRoute>
        <CheckoutPage />
    </ProtectedRoute>
} />
```

**Davranış:**
- Auth loading'de → spinner
- Auth yoksa → Keycloak'a redirect + spinner (redirect tamamlanana kadar)
- Auth varsa → children render

## MerchantProtectedRoute (tenant + outlet)

`components/shared/MerchantProtectedRoute.tsx`:

```tsx
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
    const { data: myTenants = [], isLoading: isLoadingTenants } =
        useMyTenants(isAuthenticated && user?.isMerchant);
    const { activeTenant, setActiveTenant } = useMerchantStore();
    const location = useLocation();

    // Tek tenant varsa otomatik aktif yap
    useEffect(() => {
        if (myTenants && myTenants.length === 1 && !activeTenant) {
            setActiveTenant(myTenants[0]);
        }
    }, [myTenants, activeTenant, setActiveTenant]);

    if (!isAuthenticated || isLoadingUser || isLoadingTenants) return <LoadingSpinner />;
    if (!isAuthenticated) return <Navigate to="/" replace />;

    // Merchant değil → mağaza oluştur
    if (user && !user.isMerchant && location.pathname !== '/create-store') {
        return <Navigate to="/create-store" replace />;
    }

    // Tenant yok → oluştur
    if (!myTenants || myTenants.length === 0) {
        if (location.pathname !== '/create-store' && location.pathname !== '/merchant/select') {
            return <Navigate to="/create-store" replace />;
        }
    }

    // Tenant var ama seçili değil → seçim sayfası
    if (myTenants && myTenants.length > 0 && !activeTenant && location.pathname !== '/merchant/select') {
        return <Navigate to="/merchant/select" replace />;
    }

    return <Outlet />;
};
```

**Outlet pattern:** Children prop almıyor, route hierarchy'sinde child route'lar için yer tutucu döner:

```tsx
<Route element={<MerchantProtectedRoute />}>
    <Route path="/merchant" element={<MerchantLayout />}>
        <Route index element={<MerchantDashboard />} />
        <Route path="products" element={<MerchantProducts />} />
        ...
    </Route>
</Route>
```

`<Outlet />` MerchantLayout'u render eder, layout içindeki `<Outlet />` de child route'u render eder (nested).

## ImageUploadField — single + multi

`components/shared/ImageUploadField.tsx`. İki export:

### `SingleImageUpload`

```tsx
import { SingleImageUpload } from 'components/shared/ImageUploadField';
import { ImagePreview, createImagePreviewFromUrl } from 'utils/imageUploadUtils';

const [mainImage, setMainImage] = useState<ImagePreview | null>(
    initialUrl ? createImagePreviewFromUrl(initialUrl) : null
);
const toast = useToastStore();

<SingleImageUpload
    label="Ana Görsel"
    value={mainImage}
    onChange={setMainImage}
    onError={toast.error}
/>

// Submit'te:
const handleSubmit = () => {
    if (mainImage?.uploading) {
        toast.warning('Görsel yükleniyor, lütfen bekleyin');
        return;
    }
    const payload = {
        ...other,
        mainImageUrl: mainImage?.url ?? '',
    };
    mutation.mutate(payload);
};
```

### `MultiImageUpload`

```tsx
import { MultiImageUpload } from 'components/shared/ImageUploadField';

const [images, setImages] = useState<ImagePreview[]>(
    initialUrls.map(createImagePreviewFromUrl)
);

<MultiImageUpload
    label="Galeri"
    values={images}
    onChange={setImages}
    onError={toast.error}
    max={8}
/>

// Submit'te:
// onChange filter zaten yapıyor (uploading değil + url var olanları)
const payload = {
    ...other,
    imageUrls: images.map(i => i.url),
};
```

### `ImagePreview` tipi

```tsx
export interface ImagePreview {
    id: string;
    url: string;        // MinIO URL — submit'te bu gider. Upload öncesi boş.
    previewUrl: string; // Gösterim için — objectURL veya MinIO URL
    uploading: boolean;
    error?: string;
}

// Helpers
createImagePreviewFromUrl(url)       // mevcut MinIO URL'inden oluştur (edit mode)
createImagePreviewPlaceholder(file)  // file'dan placeholder (objectURL ile preview, uploading: true)
```

### Validation

```tsx
export const ACCEPTED_IMAGE_TYPES = 'image/jpeg,image/png,image/webp';
export const MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

isValidImageType(file)  // boolean
isValidImageSize(file)  // boolean
```

## NotificationProvider + ToastContainer

İki paralel notification sistemi:

### 1. `NotificationProvider` (Snackbar tek)

`components/shared/NotificationProvider.tsx`:

```tsx
const { notify } = useNotification();
notify('Mesaj', 'success');  // 'info' | 'success' | 'error' | 'warning'
```

Auto-hide 6 saniye. Tek snackbar görünür.

### 2. `useToastStore` + `ToastContainer` (queue)

```tsx
const toast = useToastStore();
toast.success('Mesaj');
toast.error('Hata');
toast.warning('Uyarı');
toast.show('Bilgi', 'info');
```

Toast queue — birden fazla aynı anda görünür. Auto-hide 4 saniye.

**Hangisini kullan?** Yeni kodda `useToastStore` tercih et — store-based daha esnek, hook'lardan kullanılabilir (Provider sarması yok).

## BaseCard, Button, Input, ErrorState (shared)

`components/shared/`:

- `BaseCard` — Paper wrapper, başlık + içerik
- `Button` — MUI Button override
- `Input` — MUI TextField override
- `ErrorState` — error fallback UI
- `LoadingSpinner` — yüklenme spinner'ı
- `Footer`, `Header` — layout component'leri
- `PageLayout` — sayfa wrapper (header + outlet + footer)

Yeni custom component eklerken `components/shared/`'a koy — tüm tarafların kullanması için.

## Form pattern (validation manuel)

Form lib YOK. `useState` + manuel validate. `AddressFormModal.tsx` örnek:

```tsx
const [formData, setFormData] = useState<CreateAddressRequest>(emptyState);

useEffect(() => {
    if (open) {
        if (initialData) {
            setFormData({ /* initialData'dan map'le */ });
        } else {
            setFormData(emptyState);
        }
    }
}, [open, initialData]);

const handleChange = (field: keyof CreateAddressRequest) => (e: ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
};

const handleSubmit = async () => {
    // Manuel validation
    if (!formData.label.trim()) {
        toast.error('Adres etiketi zorunlu');
        return;
    }
    // ...
    await onSubmit(formData);
};
```

**Kurallar:**
- `emptyState` sabiti tanımla — reset için
- `useEffect`'te open/initialData değişince form sıfırla
- `handleChange` factory fonksiyon, field name'i parametre olarak alır
- Validation submit'te yapılır, inline (form lib gerek yok şu an)

## ESLint kuralları

`max-warnings 0` — uyarı bile build kırar.

```bash
npm run lint
```

`// eslint-disable-next-line` kullanırken sebep yorumla:

```tsx
// eslint-disable-next-line react-hooks/exhaustive-deps
}, [items]);  // onChange parent'dan, dep'e koymak loop yapar

// eslint-disable-next-line @typescript-eslint/no-explicit-any
queryClient.setQueryData(BASKET_QUERY_KEY, (old: any) => {
```

## Tuzaklar

### Provider sırası bozulması

```tsx
// ❌ AuthProvider içinde QueryClientProvider olursa, useAuth React Query'den önce çağrılır
<QueryClientProvider>
    <AuthProvider>  // ← AuthProvider içeride
        <App />
    </AuthProvider>
</QueryClientProvider>

// ✅ AuthProvider DIŞARIDA — main.tsx'teki sıra
```

### `useEffect` deps'te store action'ı

```tsx
// ❌ setAuth, clearAuth değişmediği için sonsuz loop yapmaz, ama lint warn verir
useEffect(() => { /* ... */ }, [auth.isAuthenticated]);

// ✅ Action'ları da deps'e ekle (Zustand action'ları stable referanslı)
useEffect(() => { /* ... */ }, [auth.isAuthenticated, setAuth, clearAuth]);
```

### `import.meta.env.VITE_*` typing

```tsx
// vite-env.d.ts'te tanımla:
/// <reference types="vite/client" />
interface ImportMetaEnv {
    readonly VITE_API_BASE_URL: string;
    readonly VITE_KEYCLOAK_URL: string;
    readonly VITE_KEYCLOAK_REALM: string;
    readonly VITE_KEYCLOAK_CLIENT_ID: string;
}
```

### `BrowserRouter` SSR uyumsuzluğu

Vite SPA olduğu için `BrowserRouter` sorun değil. Server-side rendering eklenirse `StaticRouter`'a geçiş gerekir.

### `useMemo` prematüre optimizasyon

```tsx
// ❌ Her şeyi useMemo'ya sarma — gereksiz overhead
const value = useMemo(() => ({ a, b }), [a, b]);  // basit obje için anlamsız

// ✅ Context provider value'su veya pahalı hesaplama için kullan
const expensive = useMemo(() => filterAndSort(items), [items]);
```

### Toast spam

```tsx
// ❌ Her tıklamada yeni toast → ekran dolu olur
button.onClick = () => toast.success('Tıklandı');

// ✅ Mutation onSuccess'te bir kez
useMutation({
    mutationFn: ...,
    onSuccess: () => toast.success('Kaydedildi'),
    onError: (e) => toast.error(e.message),
});
```

## İlişkili dosyalar

- `utils/customTheme.ts` — MUI theme
- `utils/idempotencyUtils.ts` — UUID üretimi + header sabiti
- `utils/imageUploadUtils.ts` — `ImagePreview`, validation
- `utils/routes.ts` — route sabitleri
- `lib/axios.ts` — axios instance + interceptor
- `main.tsx` — provider hiyerarşisi
- `App.tsx` — lazy routes + Suspense
- `components/shared/ProtectedRoute.tsx` — auth gate
- `components/shared/MerchantProtectedRoute.tsx` — tenant gate (Outlet)
- `components/shared/ImageUploadField.tsx` — Single + Multi upload
- `components/shared/NotificationProvider.tsx` — snackbar provider
- `components/shared/ToastContainer.tsx` — toast queue UI
- `components/customer/AddressFormModal.tsx` — form pattern referans
- `features/tenant/components/MerchantProductForm.tsx` — büyük form referans
- Skill: `frontend-data-fetching` (React Query + idempotency)
- Skill: `frontend-state-management` (Zustand stores)
