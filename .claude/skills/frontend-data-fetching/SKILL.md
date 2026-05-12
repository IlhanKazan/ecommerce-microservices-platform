---
name: frontend-data-fetching
description: Use when working with React Query (TanStack Query) hooks — useQuery, useMutation, query keys, invalidation, optimistic updates, idempotency keys for mutations, or API service files (productService, tenantService, userService, basketService). Covers the existing patterns in the IlhanKazan frontend.
---

# Frontend Data Fetching Pattern

Bu skill IlhanKazan frontend'inde React Query + axios + idempotency-key pattern'leri için referans. Mevcut kod tabanından türetildi (`query/useProductQueries.ts`, `query/useBasketQueries.ts`, `query/queryKeys.ts`, `lib/axios.ts`, `features/*/api/*.ts`).

## Üç katman

```
[Hook]            useGetProductDetail, useCreateReview, ...
   ↓ kullanır
[API service]     productService.getProductDetail, productService.createReview
   ↓ axios çağrısı
[axios instance]  lib/axios.ts (interceptor: token, 401 cleanup)
   ↓ HTTP
[Backend]
```

**Kural:** Component **doğrudan** axios çağırmaz, **doğrudan** service çağırmaz. Hep hook üzerinden gider.

## Query keys (sabit, typed, kategorize)

`query/queryKeys.ts`:

```tsx
export const QueryKeys = {
    SEARCH_PRODUCTS: (body: object) => ['searchProducts', body] as const,
    PRODUCT_DETAIL: (id: number) => ['productDetail', id] as const,
    PRODUCT_REVIEWS: (productId: number, page: number, size: number) =>
        ['product-reviews', productId, page, size] as const,
    CATEGORIES: ['categories'] as const,
    TENANT_PRODUCTS: (tenantId: number, page: number, size: number) =>
        ['tenant-products', tenantId, page, size] as const,
    CART: ['cart'] as const,
    WAREHOUSES: (tenantId: number) => ['warehouses', tenantId] as const,
};
```

**Kural:**
- Her key `as const` — TypeScript readonly tuple olarak görür, invalidation tip-güvenli
- Parametre alan key'ler **fonksiyon**, sabit key'ler **dizi**
- Kategorize edilmiş prefix kullan (`'product-reviews'`, `'tenant-products'`) — invalidation'da prefix-based çalışır
- `useBasketQueries.ts`'te ayrı `BASKET_QUERY_KEY` var — ileride `queryKeys.ts`'e konsolide edilebilir, ama şu an çalışıyor

### Invalidation pattern

```tsx
// Spesifik bir query
queryClient.invalidateQueries({ queryKey: QueryKeys.PRODUCT_DETAIL(productId) });

// Bir prefix'in altındaki TÜM query'ler
queryClient.invalidateQueries({ queryKey: ['product-reviews', productId] });
// ↑ tüm sayfa/size kombinasyonlarını invalidate eder
```

## Read pattern — useQuery

```tsx
export const useGetProductDetail = (productId: number) => {
    return useQuery({
        queryKey: QueryKeys.PRODUCT_DETAIL(productId),
        queryFn: () => productService.getProductDetail(productId),
        enabled: !!productId,
        staleTime: 1000 * 60 * 2,
    });
};
```

**Anahtarlar:**
- `enabled: !!productId` — id falsy iken fetch atlanır (route param'ı henüz hazır değilken)
- `staleTime` — kategoriye göre:
  - **Hızlı değişen** (sepet, ürün listesi): `30_000` (30s)
  - **Orta** (ürün detayı, profil): `1000 * 60 * 2` (2dk) — `1000 * 60 * 5` (5dk)
  - **Yavaş** (kategori ağacı): `1000 * 60 * 30` (30dk)
  - **Edit form** açılışında taze: `staleTime: 0`

### Default'lar (main.tsx)

```tsx
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 60_000,             // 1 dakika
            retry: 1,                       // 1 kez retry
            refetchOnWindowFocus: false,   // window focus'ta tekrar çekmez
        },
    },
});
```

Hook bazında override edilir.

## Write pattern — useMutation + idempotency-key

**Bu pattern'in nedeni:** Backend `@Idempotent` AOP ile Redis dedupe yapıyor. Aynı `Idempotency-Key` ile gelen ikinci istek 400 döner. Frontend tarafında:

- Kullanıcı çift tıkladığında → aynı key ile iki POST → backend ikinciyi reddeder
- Ağ hatası retry'da → aynı key, backend "zaten işlendi" der → güvenli idempotency
- Yeni işlem → yeni key → backend dedupe etmez

### Pattern (zorunlu)

```tsx
import { useRef } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { generateIdempotencyKey } from '../utils/idempotencyUtils';

export const useCreateReview = (productId: number) => {
    const queryClient = useQueryClient();
    const idempotencyKey = useRef(generateIdempotencyKey());

    return useMutation({
        mutationFn: (body: ReviewCreateRequest) =>
            productService.createReview(productId, body, idempotencyKey.current),

        onSuccess: () => {
            // Bir sonraki işlem için yeni key
            idempotencyKey.current = generateIdempotencyKey();

            // Cache invalidation
            queryClient.invalidateQueries({ queryKey: ['product-reviews', productId] });
            queryClient.invalidateQueries({ queryKey: QueryKeys.PRODUCT_DETAIL(productId) });
        },

        retry: 2,
    });
};
```

### Key lifecycle (önemli!)

| Olay | Key davranışı | Sonuç |
|---|---|---|
| Hook mount | Yeni key üretilir | İlk işlem benzersiz |
| `onSuccess` | Yeni key üretilir | Bir sonraki işlem farklı |
| `onError` | Aynı kalır | Retry aynı key ile gider, backend dedupe eder |
| Component unmount/mount | Yeni key | Form kapatıp açılırsa yeni key |
| `retry` (otomatik) | Aynı kalır | Backend dedupe çalışır |

### `useRef` kritik — `useState` kullanma

```tsx
// ❌ YANLIŞ
const [key, setKey] = useState(generateIdempotencyKey());
// setState her render'da component yeniden render olunca state aynı kalır,
// ama setKey çağırınca render → re-render → mutation hook yeniden çağrılır → bug

// ✅ DOĞRU
const idempotencyKey = useRef(generateIdempotencyKey());
// Re-render'da değişmez, sadece .current = ... ile manuel günceller
```

### Hangi mutation'lar idempotency-key gerektirir?

**Gerektirir:**
- `useCreateReview` — ürün yorumu
- `useCreateTenantProduct` — yeni ürün ekleme
- `useUpdateTenantProduct` — ürün güncelleme
- `useCreateWarehouse` — yeni depo
- `useAddManualStock` — **en kritik** (duplicate stok = envanter hatası)
- `useAddToBasket` — sepete ekleme
- Yeni: tenant create, payment, address create

**Gerektirmez (backend zaten idempotent):**
- DELETE — silme zaten idempotent
- PATCH (toggle gibi) — `useMarkReviewHelpful`, `useUpdateSalesStatus`
- GET — read, mutation değil

### Service-level pattern

`features/*/api/*.ts`'te helper:

```tsx
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';

function idempotencyHeader(key?: string): Record<string, string> {
    return key ? { [IDEMPOTENCY_KEY_HEADER]: key } : {};
}

// Mutation için
createReview: async (productId, body, idempotencyKey: string) => {
    const response = await api.post(
        endpoint,
        body,
        { headers: idempotencyHeader(idempotencyKey) },
    );
    return response.data;
},
```

`IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key' as const` — sabit, bir noktada.

## Optimistic update + rollback (gelişmiş)

Sepete ekleme gibi UI'nin **anında** güncellenmesi gerektiği yerler için. Pattern `useAddToBasket`'te:

```tsx
return useMutation({
    mutationFn: (newItem) => basketService.addToCart(newItem, idempotencyKey.current),

    onMutate: async (newItem) => {
        // 1. Bekleyen sorguları iptal et (yarış koşulu önleme)
        await queryClient.cancelQueries({ queryKey: BASKET_QUERY_KEY });

        // 2. Önceki state'i sakla (rollback için)
        const previousBasket = queryClient.getQueryData(BASKET_QUERY_KEY);

        // 3. UI'yi optimistic güncelle
        queryClient.setQueryData(BASKET_QUERY_KEY, (old: any) => {
            if (!old) return old;
            const existing = old.items.find((i: any) => i.productId === newItem.productId);
            const updatedItems = existing
                ? old.items.map((i: any) =>
                    i.productId === newItem.productId
                        ? { ...i, quantity: i.quantity + newItem.quantity }
                        : i,
                )
                : [...old.items, { productId: newItem.productId, quantity: newItem.quantity, /* ... */ }];
            return { ...old, items: updatedItems };
        });

        return { previousBasket };  // context döner
    },

    onSuccess: () => {
        idempotencyKey.current = generateIdempotencyKey();
    },

    onError: (_err, _vars, context) => {
        // Rollback
        if (context?.previousBasket) {
            queryClient.setQueryData(BASKET_QUERY_KEY, context.previousBasket);
        }
    },

    onSettled: () => {
        // Her durumda invalidate — true state'e dön
        queryClient.invalidateQueries({ queryKey: BASKET_QUERY_KEY });
    },

    retry: 1,
});
```

**Optimistic update ne zaman kullanılır:**
- UI feedback'in anında olması gerekiyor (sepete ekle, like)
- API yavaş veya unreliable
- Geri alma (rollback) yapılabilir

**Ne zaman kullanılmaz:**
- Yeni kayıt yaratma (id'yi backend dönecek)
- Server-side validation kritik (fiyat hesaplaması, stok kontrolü)
- Hata durumunda kullanıcının net bilgi alması gerekiyor

## API service dosya yapısı

`features/<feature>/api/<feature>Service.ts`:

```tsx
import { api } from '../../../lib/axios';
import { API_ENDPOINTS } from '../../../config/apiEndpoints';
import { IDEMPOTENCY_KEY_HEADER } from '../../../utils/idempotencyUtils';
import type { /* DTO types */ } from '../../../types/<feature>';

function idempotencyHeader(key?: string): Record<string, string> {
    return key ? { [IDEMPOTENCY_KEY_HEADER]: key } : {};
}

export const featureService = {
    // GET
    getX: async (id: number): Promise<XResponse> => {
        const response = await api.get<XResponse>(API_ENDPOINTS.X.BY_ID(id));
        return response.data;
    },

    // POST (idempotency-key destekli)
    createX: async (body: XCreateRequest, idempotencyKey: string): Promise<XResponse> => {
        const response = await api.post<XResponse>(
            API_ENDPOINTS.X.CREATE,
            body,
            { headers: idempotencyHeader(idempotencyKey) },
        );
        return response.data;
    },

    // PATCH (idempotent — key gerektirmez)
    toggleX: async (id: number, value: boolean): Promise<void> => {
        await api.patch(API_ENDPOINTS.X.TOGGLE(id), null, { params: { value } });
    },

    // DELETE
    deleteX: async (id: number): Promise<void> => {
        await api.delete(API_ENDPOINTS.X.DELETE(id));
    },
};
```

**Kurallar:**
- Her metod `async`, dönüş tipi tipli
- `api` instance import edilir, raw `axios` import edilmez
- Endpoint sabit `API_ENDPOINTS`'tan gelir, hard-code yasak
- Generic tipi response'a ver: `api.get<XResponse>(...)`

### Endpoint sabitleri

`config/apiEndpoints.ts`:

```tsx
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export const API_ENDPOINTS = {
    USER: {
        ME: '/users/me',
        BY_ID: (id: number) => `/users/${id}`,
        ADDRESSES: '/users/addresses',
        ADDRESS_BY_ID: (id: number) => `/users/addresses/${id}`,
    },
    PRODUCT: {
        BY_ID_PUBLIC: (id: number) => `/public/products/${id}`,
        TENANT_LIST: (tenantId: number) => `/products/tenants/${tenantId}`,
        TENANT_CREATE: (tenantId: number) => `/products/tenants/${tenantId}`,
        // ...
    },
    // ...
} as const;
```

`as const` — Type güvenliği için kritik. Hard-code endpoint asla yok.

## axios setup (`lib/axios.ts`)

```tsx
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { API_BASE_URL } from '../config/apiEndpoints';
import { useAuthStore } from '../store/useAuthStore';

const addAuthInterceptor = (instance: AxiosInstance) => {
    instance.interceptors.request.use(
        (config: InternalAxiosRequestConfig) => {
            const token = useAuthStore.getState().token;
            if (token && config.headers) {
                config.headers.Authorization = `Bearer ${token}`;
            }
            return config;
        },
        (error) => Promise.reject(error),
    );

    instance.interceptors.response.use(
        (response) => response,
        (error) => {
            if (error.response?.status === 401) {
                console.error('Yetkisiz erişim! Token geçersiz veya süresi dolmuş.');
                useAuthStore.getState().clearAuth();
            }
            return Promise.reject(error);
        },
    );
};

export const api = axios.create({
    baseURL: API_BASE_URL,
    headers: { 'Content-Type': 'application/json' },
});

addAuthInterceptor(api);
```

**Önemli:** Idempotency-Key burada YOK. Her hook kendi key'ini yönetir (yukarıdaki `useRef` pattern).

## Tuzaklar

### `enabled` koşulu unutmak

```tsx
// ❌ id undefined iken fetch atılır → 400
const { data } = useGetProductDetail(productId);

// ✅
const { data } = useGetProductDetail(productId);
// Hook içinde: enabled: !!productId
```

### `useState` ile idempotency key

```tsx
// ❌ Re-render'da setState çağrılırsa key yenilenir, retry farklı key alır
const [key] = useState(generateIdempotencyKey());

// ✅
const idempotencyKey = useRef(generateIdempotencyKey());
```

### `onSuccess`'te key yenilenmemesi

```tsx
// ❌ Aynı key her isteğe gider — ikinci farklı body için backend "duplicate" der
return useMutation({
    mutationFn: (body) => service.create(body, idempotencyKey.current),
    onSuccess: () => {
        // Key yenilemedik!
        queryClient.invalidateQueries(...);
    },
});

// ✅
onSuccess: () => {
    idempotencyKey.current = generateIdempotencyKey();
    queryClient.invalidateQueries(...);
},
```

### `setQueryData` direkt mutation

```tsx
// ❌ Server state'i tetiklemeden değiştirme — invalidate yok, gerçek server state'i karışır
queryClient.setQueryData(['cart'], newCart);

// ✅ Optimistic update yapacaksan onMutate + onError + onSettled tam zinciri kur
```

### Query key formatı tutarsızlığı

```tsx
// ❌ Farklı yerlerde farklı format
queryClient.invalidateQueries({ queryKey: ['products', id] });
queryClient.invalidateQueries({ queryKey: QueryKeys.PRODUCT_DETAIL(id) });

// ✅ queryKeys.ts'de tek yerden yönet
```

### `staleTime: 0` herkese

```tsx
// ❌ Her istek backend'e gider, cache anlamsız
useQuery({ queryFn: ..., staleTime: 0 });

// ✅ Default 60s, edit formlarında 0
```

## İlişkili dosyalar

- `query/queryKeys.ts` — sabit query key tanımları
- `query/useProductQueries.ts` — idempotency hook pattern referans
- `query/useBasketQueries.ts` — optimistic update + rollback referans
- `query/useTenantQueries.ts`, `query/useUserQueries.ts` — basit read hook'ları
- `lib/axios.ts` — instance + interceptor
- `utils/idempotencyUtils.ts` — `generateIdempotencyKey` + header sabiti
- `features/catalog/api/productService.ts` — API service pattern
- `config/apiEndpoints.ts` — endpoint sabitleri
- Skill: `frontend-state-management` (Zustand tarafı)
- Skill: `frontend-component-patterns` (form + image upload)
