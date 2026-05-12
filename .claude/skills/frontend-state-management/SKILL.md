---
name: frontend-state-management
description: Use when working with Zustand stores — useAuthStore, useCartStore, useMerchantStore, useCategoryStore, useToastStore — or implementing client-side state. Covers persist middleware, partialize, cross-store cleanup (auth → cart), guest cart → backend merge, and selector patterns.
---

# Frontend State Management Pattern (Zustand)

Bu skill IlhanKazan frontend'inde Zustand store'lar ve client state yönetimi için referans. Mevcut store'lardan türetildi (`store/useAuthStore.ts`, `useCartStore.ts`, `useMerchantStore.ts`, `useCategoryStore.ts`, `useToastStore.ts`).

## Server state vs Client state

| Tip | Yönetim | Örnek |
|---|---|---|
| **Server state** | React Query | Ürün listesi, sepet detayı, kullanıcı profil bilgisi |
| **Client state** | Zustand | Auth token, guest cart, active tenant, toast queue, kategori cache |
| **Form state** | useState/useReducer | Formdaki input değerleri |

**Karar kriteri:** State **API'den geliyor mu?** → Server state (React Query). **Sadece UI mı?** → Client state (Zustand). **Sayfa içinde geçici mi?** → useState.

## Mevcut store'lar

### 1. `useAuthStore` — token + user + isAuthenticated

```tsx
import { create } from 'zustand';
import { User as OidcUser } from 'oidc-client-ts';
import type { User as AppUser } from '../types/user';

interface AuthState {
    token: string | null;
    oidcProfile: OidcUser['profile'] | null;
    user: AppUser | null;
    isAuthenticated: boolean;
    setAuth: (token: string, oidcProfile: OidcUser['profile']) => void;
    setUser: (user: AppUser) => void;
    clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: null,
    oidcProfile: null,
    user: null,
    isAuthenticated: false,

    setAuth: (token, oidcProfile) => set({
        token,
        oidcProfile,
        isAuthenticated: true,
    }),

    setUser: (user) => set({ user }),

    clearAuth: () => {
        // Cross-store cleanup — circular import önlemek için dynamic import
        import('./useCartStore').then(({ useCartStore }) => {
            useCartStore.getState().clearCart();
        });

        set({
            token: null,
            oidcProfile: null,
            user: null,
            isAuthenticated: false,
        });
    },
}));
```

**Kritik pattern: cross-store cleanup**
- `clearAuth()` çağrıldığında `useCartStore.clearCart()` da çağrılmalı (kullanıcı çıkınca sepet temizlensin)
- `useAuthStore` üstten `useCartStore` import ETMEZ — circular dependency olur
- **Dynamic `import()` kullanılır** — runtime'da çözülür, build hatası vermez

**Persist YOK** — token bilinçli olarak persist edilmiyor. OIDC kendi storage'ını yönetiyor (oidc-client-ts), `setAuth` her oturumda yeniden çağrılıyor. localStorage'a token koymak güvensiz (XSS).

### 2. `useCartStore` — guest cart + persist

```tsx
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export interface CartItem {
    productId: number;
    name: string;
    price: number;
    quantity: number;
    mainImageUrl: string | null | undefined;
}

interface CartState {
    items: CartItem[];
    addItem: (item: CartItem) => void;
    removeItem: (productId: number) => void;
    updateQuantity: (productId: number, quantity: number) => void;
    clearCart: () => void;
    getTotalPrice: () => number;
    getItemCount: () => number;
}

export const useCartStore = create<CartState>()(
    persist(
        (set, get) => ({
            items: [],

            addItem: (newItem) => set((state) => {
                const existing = state.items.find(i => i.productId === newItem.productId);
                if (existing) {
                    return {
                        items: state.items.map(i =>
                            i.productId === newItem.productId
                                ? { ...i, quantity: i.quantity + newItem.quantity }
                                : i
                        ),
                    };
                }
                return { items: [...state.items, newItem] };
            }),

            removeItem: (productId) => set((state) => ({
                items: state.items.filter(i => i.productId !== productId),
            })),

            updateQuantity: (productId, quantity) => set((state) => ({
                items: state.items.map(i =>
                    i.productId === productId ? { ...i, quantity } : i
                ),
            })),

            clearCart: () => set({ items: [] }),

            getTotalPrice: () => get().items.reduce((sum, i) => sum + i.price * i.quantity, 0),
            getItemCount: () => get().items.reduce((sum, i) => sum + i.quantity, 0),
        }),
        {
            name: 'guest-cart',  // localStorage key
        }
    )
);
```

**Kritik pattern'ler:**

- `persist` middleware — guest kullanıcı sepete ürün eklediği zaman browser kapanıp açılınca sepet kalsın
- `name: 'guest-cart'` — localStorage key
- Computed values (`getTotalPrice`, `getItemCount`) `get()` ile diğer state'e erişir
- `addItem` içinde duplicate kontrolü — aynı productId varsa quantity ekle, yoksa yeni item

### 3. `useCartStore` ile guest → auth merge (App.tsx)

Login olunca guest cart backend'e push edilir, sonra clear. **Bu kritik bir akış**, hatalı yapılırsa data kaybı olur.

```tsx
// App.tsx
function App() {
    const auth = useAuth();  // react-oidc-context
    const setAuth = useAuthStore((state) => state.setAuth);
    const clearAuth = useAuthStore((state) => state.clearAuth);

    const { items: localCartItems, clearCart } = useCartStore();
    const { mutate: addToBasket } = useAddToBasket();

    useEffect(() => {
        if (auth.isAuthenticated && auth.user?.access_token) {
            setAuth(auth.user.access_token, auth.user.profile);

            // GUEST CART SENKRONİZASYONU
            if (localCartItems.length > 0) {
                localCartItems.forEach(item => {
                    addToBasket({ productId: item.productId, quantity: item.quantity });
                });
                clearCart();  // Backend'e geçince local'i sıfırla
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
    // ↑ localCartItems, addToBasket, clearCart deps'te yok — kasıtlı
    // çünkü cart-add bittikten sonra invalidate olunca tekrar tetiklerdi
}
```

**Bilinen kayıt:** Bu pattern'in iki sorunu var (TODO'da var):
- **Idempotency:** Her `addToBasket` kendi key'ini yönetiyor ama `forEach` tüm item'ları aynı render'da batchlemiyor — eğer ağ kopuk + retry olursa duplicate gelebilir. Backend dedupe ediyor, sorun değil.
- **Stok kontrolü:** Login'den önce sepete eklenen ürünün stoku bitmiş olabilir. Backend hata dönecek, frontend toast gösterecek. Henüz uygulanmadı.

### 4. `useMerchantStore` — active tenant + persist + partialize

```tsx
import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { TenantSummary } from '../types/tenant';

interface MerchantState {
    activeTenant: TenantSummary | null;
    setActiveTenant: (tenant: TenantSummary) => void;
    clearMerchantSession: () => void;
}

export const useMerchantStore = create<MerchantState>()(
    persist(
        (set) => ({
            activeTenant: null,
            setActiveTenant: (tenant) => set({ activeTenant: tenant }),
            clearMerchantSession: () => set({ activeTenant: null }),
        }),
        {
            name: 'merchant-storage',
            partialize: (state) => ({ activeTenant: state.activeTenant }),
            // ↑ sadece activeTenant'ı persist et, action'ları persist etme
        }
    )
);
```

**`partialize` ne zaman lazım:**
- Default'ta tüm state persist olur, action'lar dahil
- Function (action) JSON serialize edilemez, persist'te garip davranışlar olur
- `partialize: (state) => ({ ...state })` ile sadece **state**'i seç, action'ları persist'tan çıkar

**Tenant list KULLANIM HATIRLATMA:**
- Tenant **list**'i bu store'da tutulmaz, `useMyTenants()` hook'u (React Query) ile gelir
- Bu store sadece **active selection**'ı tutar (UI state)
- Multi-tenant kullanıcı için: `MerchantProtectedRoute` 1 tenant varsa otomatik active yapar, çok tenant varsa `/merchant/select`'e yönlendirir

### 5. `useCategoryStore` — global category cache

```tsx
import { create } from 'zustand';
import type { CategoryResponse } from '../types/product';

interface CategoryState {
    categories: CategoryResponse[];
    isLoaded: boolean;
    setCategories: (cats: CategoryResponse[]) => void;
}

export const useCategoryStore = create<CategoryState>((set) => ({
    categories: [],
    isLoaded: false,
    setCategories: (cats) => set({ categories: cats, isLoaded: true }),
}));
```

**Pattern:** Kategori ağacı uygulama açılışında bir kez fetch edilip global tutuluyor. App.tsx'te:

```tsx
const { data: categoryData } = useGetCategories();
const setCategories = useCategoryStore((state) => state.setCategories);

useEffect(() => {
    if (categoryData) {
        setCategories(categoryData);
    }
}, [categoryData, setCategories]);
```

Her sayfada `useGetCategories()` tekrar çağrılmıyor, store'dan okunuyor:

```tsx
const categories = useCategoryStore((state) => state.categories);
```

### 6. `useToastStore` — bildirim queue

```tsx
import { create } from 'zustand';

type ToastSeverity = 'success' | 'error' | 'warning' | 'info';

interface Toast {
    id: number;
    message: string;
    severity: ToastSeverity;
}

interface ToastState {
    toasts: Toast[];
    show: (message: string, severity?: ToastSeverity) => void;
    success: (message: string) => void;
    error: (message: string) => void;
    warning: (message: string) => void;
    dismiss: (id: number) => void;
}

let toastId = 0;

export const useToastStore = create<ToastState>((set) => ({
    toasts: [],

    show: (message, severity = 'info') => {
        const id = ++toastId;
        set((state) => ({
            toasts: [...state.toasts, { id, message, severity }],
        }));
        setTimeout(() => {
            set((state) => ({ toasts: state.toasts.filter(t => t.id !== id) }));
        }, 4000);
    },

    success: (message) => useToastStore.getState().show(message, 'success'),
    error: (message) => useToastStore.getState().show(message, 'error'),
    warning: (message) => useToastStore.getState().show(message, 'warning'),

    dismiss: (id) => set((state) => ({
        toasts: state.toasts.filter(t => t.id !== id),
    })),
}));
```

**Kullanım:**
```tsx
import { useToastStore } from '../store/useToastStore';

const toast = useToastStore();

try {
    await mutation.mutateAsync(...);
    toast.success('Başarılı!');
} catch (e) {
    toast.error('Hata oluştu');
}
```

**Component yan tarafı:** `<ToastContainer />` `App.tsx`'te bir kez render edilir, `useToastStore.toasts`'u dinler ve render eder.

## Selector pattern (ÖNEMLİ — performans)

```tsx
// ❌ Tüm state — herhangi bir field değişince component re-render
const cart = useCartStore();
const { items } = cart;

// ✅ Sadece items'ı seç — sadece items değişince re-render
const items = useCartStore((state) => state.items);

// ✅ Birden fazla field — shallow eşitlik
import { useShallow } from 'zustand/react/shallow';
const { items, addItem } = useCartStore(
    useShallow((state) => ({ items: state.items, addItem: state.addItem }))
);

// ✅ Action'lar zaten referans-stable, yan yana destructure edilebilir
const addItem = useCartStore((state) => state.addItem);
const removeItem = useCartStore((state) => state.removeItem);
```

**Kural:** Component **sadece kullandığı** field'ı seçsin. `getState()` ile çekme dışarıdan (component dışında):

```tsx
// Component dışında
const token = useAuthStore.getState().token;
```

## Yeni store yazma — şablon

```tsx
import { create } from 'zustand';
import { persist } from 'zustand/middleware';  // persist gerekiyorsa

interface XState {
    // state
    field: SomeType;

    // actions
    setField: (value: SomeType) => void;
    clear: () => void;
}

export const useXStore = create<XState>()(
    // persist sarması opsiyonel
    persist(
        (set, get) => ({
            field: initialValue,

            setField: (value) => set({ field: value }),

            clear: () => set({ field: initialValue }),
        }),
        {
            name: 'x-storage',
            partialize: (state) => ({ field: state.field }),
        }
    )
);
```

**Karar noktaları:**

| Soru | Cevap |
|---|---|
| Persist'lensin mi? | Browser refresh'te kalmasını istiyorsan: evet |
| Hangi field'lar persist? | `partialize` ile sadece state, action'lar hariç |
| Cross-store cleanup gerekir mi? | Auth değişince temizlenmeli mi? Evet ise `useAuthStore.clearAuth` içinden dynamic import |
| Computed değerler? | `get()` ile diğer state'e erişen fonksiyonlar (`getTotalPrice` gibi) |
| Async action? | Action içinde async işlem yap, set'i sonunda çağır |

## Tuzaklar

### Cross-store import — circular dependency

```tsx
// ❌ Top-level import → A → B → A circular
import { useCartStore } from './useCartStore';

// ✅ Dynamic import → runtime resolution
import('./useCartStore').then(({ useCartStore }) => {
    useCartStore.getState().clearCart();
});
```

### Selector kullanmamak

```tsx
// ❌ Tüm state — re-render felaketi
const auth = useAuthStore();

// ✅ Sadece gereken
const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
```

### Persist + computed

```tsx
// ❌ Computed değerler persist'lenir, sonsuz loop
persist((set, get) => ({
    items: [],
    total: 0,  // ← state'te tut
}));

// ✅ Computed get() ile çağrıda hesaplanır, persist'lenmez
persist((set, get) => ({
    items: [],
    getTotalPrice: () => get().items.reduce(...),  // ← fonksiyon
}));
```

### `set` içinde async — yanlış

```tsx
// ❌ set içinde await çalışmaz
addItemAsync: async (item) => {
    set(async (state) => {
        const validated = await validateItem(item);
        return { items: [...state.items, validated] };
    });
},

// ✅ Önce async iş, sonra set
addItemAsync: async (item) => {
    const validated = await validateItem(item);
    set((state) => ({ items: [...state.items, validated] }));
},
```

### Direkt mutate (immutability bozma)

```tsx
// ❌ items'ı direkt değiştirme — Zustand referans-eşitlik check eder, render olmaz
addItem: (item) => set((state) => {
    state.items.push(item);
    return state;
}),

// ✅ Yeni array
addItem: (item) => set((state) => ({
    items: [...state.items, item],
})),
```

### App.tsx merge useEffect deps

```tsx
useEffect(() => {
    if (auth.isAuthenticated) {
        // ...
        localCartItems.forEach(item => addToBasket(...));
        clearCart();
    }
}, [auth.isAuthenticated, auth.user, auth.error, setAuth, clearAuth]);
// ↑ localCartItems DEP'TE OLMAMALI — clearCart sonrası state değişir, tekrar tetiklenir
```

## İlişkili dosyalar

- `store/useAuthStore.ts` — auth state + cross-store cleanup
- `store/useCartStore.ts` — guest cart + persist
- `store/useMerchantStore.ts` — active tenant + partialize
- `store/useCategoryStore.ts` — global cache pattern
- `store/useToastStore.ts` — bildirim queue
- `App.tsx` — guest cart → auth merge useEffect
- Skill: `frontend-data-fetching` (server state tarafı)
- Skill: `frontend-component-patterns` (toast container, image upload)
