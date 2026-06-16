# TODO.md

Bu dosya **aktif sprint** odaklıdır. Genel bilgi için kardeş dosyalar:

- 📋 `TECHNICAL-DEBT.md` — kategori bazında bilinen borç (güvenlik, prod-blocker, kalite, nice-to-have)
- 🛠️ `SERVICE-WORK.md` — servis bazında yapılacak iş (endpoint, business logic, future)

İş bu üçü arasında akar:

```
SERVICE-WORK / TECHNICAL-DEBT  →  TODO.md "Aktif"  →  TODO.md "✅ Tamamlanmış"
        (backlog)                   (sprint)              (tarihçe)
```

---

## ✅ Tamamlanmış

Üzerine dönme.

- **Stage 1: product-service event akışı** ÇALIŞIYOR — outbox → Debezium → Kafka → Elasticsearch
- **Stage 2: search-service consumer event-contracts'a geçti** — header tabanlı dispatch
- **common-lib zenginleştirme:**
  - `BaseInbox` (status/retry/error/received_at), `BaseOutbox`, `InboxStatus` enum common'a
  - `@Idempotent` AOP, `@CurrentUser` resolver, `TenantSecurityEvaluator`, `FeignClientInterceptor`
- **product-service Inbox** `BaseInbox` extend ediyor
- **stock-service migration (Stage 3):** Outbox UUID→BIGINT, Inbox enrichment, REPLICA IDENTITY, replication slot rebuilt, `event-contracts` kullanımı
- **Stage 4.1: payment-service migration (kod tarafı)** — Outbox/Inbox refactored, OutboxService, payment connector, V3 migration
- **Stage 4.2: user-tenant-service migration (kod tarafı)** — Outbox refactored, V4, OutboxService, tenant event'leri (CREATED, ACTIVATED, PAYMENT_FAILED)
- **Stage 5: payment + user-tenant runtime verify** — connector status RUNNING, mesajlar `message_type` header'lı PAYMENT/TENANT topic'lerine düşüyor ✅
- **Stage 6.1: product-service ImageService** kod tarafı tamamlandı (UTS pattern'i kopyalandı)
- **Stage 6.2: product-service image upload runtime verify** ✅ — MinIO `products/` bucket oluştu, upload çalışıyor. Frontend URL fix (`tenantId` eksikti), multipart Content-Type fix (global axios header eziyordu)
- **Stage 7: ImageService iyileştirmeleri** ✅ — RuntimeException→BusinessException/SystemException, validation (5MB/content-type/empty), try-with-resources, filename sanitization, @PostConstruct bucket check, UriComponentsBuilder URL build — UTS + product-service ikisinde
- **FB-2: Tenant product detail endpoint'i** ✅ — `ProductDetailInfo` + `GET /tenants/{tenantId}/{productId}/detail`
- **FB-4: Frontend ürün ekleme/düzenleme UI** ✅ — Edit formda `/detail` endpoint, tüm field'lar (weightGrams, seo, tags…) form state'te, veri kaybı yok
- **Stage 8: mail-service MVP** ✅
- **Stage 9: Tüm servisler containerize edildi** ✅ — Her servis için Dockerfile (BuildKit cache mount, non-root user) + application-prod.yml (HikariCP, JWT dual-mode, Flyway retry, Zipkin) + docker-compose wiring. connector-init `docker-init-connectors.sh`'e taşındı, `.env.example` tüm değişkenlerle yazıldı, boşluklu YAML key'ler (`resource server` → `resourceserver`) düzeltildi.
- **Stage 11: Observability** ✅ — Prometheus/Grafana/Zipkin/Loki/Promtail/cAdvisor ana compose'a taşındı, container DNS scrape, Grafana datasource+dashboard provisioning (4701 JVM + 14282 cAdvisor), Loki persistent volume, ES healthcheck, search-service timing fix.
- **Stage 10: Order Service (backend)** ✅ — Pay-First Choreography SAGA. Checkout (reserve → pay → save+outbox), cancel akışı, async stok commit/rollback (Kafka), compensation (STOCK_COMMIT_FAILED → refund), mail entegrasyonu (4 ORDER event şablonu), Debezium connector, Dockerfile + prod.yml + compose. Detay: `ORDER-SAGA-DESIGN.md`
  - Tests: `*ApplicationTests` boilerplate testleri `@Disabled` (dev ortamı gerektirir, CI'da çalışır); stock-service Testcontainers ile düzeltildi
  - Bilinen teknik borç: `TECHNICAL-DEBT.md` "Order Service" bölümüne taşındı
- **Stage 10.6 / FB-7: Order Service Frontend** ✅ — Checkout 3-adım stepper (adres→kart→onay), AccountOrders (liste+pagination+StatusChip), OrderDetailModal (items+iptal akışı), MerchantOrdersPage (tablo+filtre+kargoya ver/teslim), MerchantOrderDetailModal. React Query hooks (useCreateOrder idempotency, useCancelOrder, useGetTenantOrders, useUpdateOrderStatus). api-gateway route eklendi (dev+prod). iyzico subMerchantKey pipeline'a eklendi.
- **FB-5: Tenant depo & stok detay görünümü** ✅ — MerchantWarehousePage expand/collapse, AddStockDialog, RemoveStockDialog. Backend: `POST /manual-remove` (stock-service).
- **FB-6: Arama autocomplete + ürün görseli** ✅ — search-service `GET /public/search/autocomplete` endpoint, frontend Header.tsx MUI Autocomplete (debounced 300ms, görsel avatar, Enter korumalı).

---

## ⏭️ Aktif

### Sprint 1: Borç temizleme ✅ Tamamlandı

### Sprint 2: Platform mail sistemi tamamlama

#### S2-1: Platform mail sistemi — kod tarafı ✅ 2026-06-07
- [x] ORDER_DELIVERED event + outbox publish (order-service) + mail handler + template
- [x] SUBSCRIPTION_ACTIVATED event'i mail-service'te wire et (payload'a contactEmail eklendi)
- [x] SUBSCRIPTION_RENEWAL_SUCCESS event + outbox publish (SubscriptionRenewalProcessor)
- [x] SUBSCRIPTION_RENEWAL_FAILED event + outbox publish (suspended flag dahil)
- [x] SubscriptionMailHandler yeni bean + 3 handler metodu
- [x] mail-service PAYMENT topic @KafkaListener eklendi
- [x] 4 yeni Thymeleaf template (order-delivered, subscription-activated, renewal-success, renewal-failed)
- [x] TenantSubscription.contactEmail alanı + V7 migration
- [x] PaymentContext/PaymentProcessRequest'e contactEmail eklendi

#### S2-2: Runtime verify (kullanıcı) ✅ 2026-06-14
- [x] Build al: event-contracts → common-lib → payment-service, order-service, mail-service
- [x] `docker compose up -d --build payment-service order-service mail-service`
- [x] Merchant bir siparişi "Teslim Edildi" yap → Mailhog'da order-delivered maili
- [x] Yeni tenant oluştur → Mailhog'da hem tenant-activated hem subscription-activated (2 mail)
- [x] Renewal test: cron geçici `0/30 * * * * *` yap → Mailhog'da renewal maili

---

## 🧩 Frontend ↔ Backend arası açık talepler

### FB-1: Ürün görseli upload endpoint'i ✅
### FB-2: Tenant product detail endpoint'i ✅
### FB-4: Frontend ürün ekleme/düzenleme UI ✅
### FB-5: Tenant depo & stok detay görünümü ✅
### FB-6: Arama autocomplete + ürün görseli ✅
### FB-7: Sipariş akışı frontend ✅

### FB-3: MinIO prod reverse proxy (not, prod roadmap)
Şu an dev'de doğrudan erişim. Prod'da signed URL + access control gerekecek.

---

## 🎨 Sprint 3: Frontend iyileştirme & Review sistemi

### FB-8: Ürün sayfasında tenant kimliği ✅ 2026-06-07
- [x] product-service: `UserTenantServiceClient` + adapter → `PublicProductInfo`'ya `tenantName`/`tenantLogoUrl` eklendi
- [x] search-service: `UserTenantServiceClient` + adapter → `ProductDocument`'e `tenantName`/`tenantLogoUrl` eklendi; CREATED/UPDATED event'lerinde UTS çağrısı
- [x] Frontend: `ProductSummary` ve `ProductDetail` tiplerine field'lar eklendi
- [x] `ProductCard.tsx`: küçük Avatar + tenant adı (null-safe)
- [x] `ProductDetailPage.tsx`: "Satıcı:" satırı — Avatar + isim kutusu

### Veri tutarlılığı — ES inStock + sort ✅ 2026-06-07
- [x] product-service: `POST /api/v1/public/admin/reindex` — tüm ACTIVE ürünler için PRODUCT_UPDATED event yayınlar (AdminProductController)
- [x] stock-service: `POST /api/v1/public/admin/stocks/resync` — availableQuantity>0 olan stoklar için STOCK_STATUS_CHANGED(inStock=true) yayınlar (AdminStockController + resyncStockStatus())
- [x] search-service: `ProductEventConsumer.handleProductCreated` → `createdAt: LocalDateTime.now()` set edildi
- [x] search-service: `handleProductUpdated` → yeni document ise `createdAt` set edildi
- [x] search-service: default sort değiştirildi — `inStock desc, createdAt desc nulls last` (stokta olanlar önce)
- [x] `HomePage.tsx` + `ProductListPage.tsx`: `inStock: true` zorunlu filtresi kaldırıldı (tüm ürünler listelenir, kart "STOKTA YOK" rozetiyle ayırt eder)
- **Build gerekiyor:** stock-service + search-service → `docker compose up -d --build stock-service search-service`

### FB-9: Review sistemi (mock + entegrasyon) ✅ 2026-06-10 (kod tarafı)
**Çözüm:** product-service `review/` paketi — full stack.
- [x] Backend: `ProductReview` + `ReviewVote` entity, `ReviewStatus` enum, `ReviewRepository`/`ReviewVoteRepository`
  - `POST /api/v1/public/products/{productId}/reviews` — yorum ekle (auth) + görsel upload endpoint'i
  - `GET /api/v1/public/products/{productId}/reviews` — sayfalı liste (public)
  - `PATCH .../reviews/...` (vote/seller response), `DELETE .../reviews/...`
  - `InternalReviewController PATCH /reviews/{id}/sentiment` (AI sentiment için altyapı hazır)
- [x] Frontend: `ProductDetailPage` yıldız `Rating` + yorum listesi + yorum formu (görsel ekli), `useGetProductReviews` + create mutation (`useProductQueries`)
- [ ] Mock data üretici (seed) — yapılmadı, opsiyonel (review sistemi onsuz çalışıyor)
- [x] **Runtime verify (kullanıcı):** ✅ yorum ekle → listede gör → rating ortalaması ürün detayda güncellensin

### FB-10: Public tenant tanıtım sayfası ✅ 2026-06-10 (kod tarafı)
**Çözüm:** storefront endpoint + StorePage.
- [x] Backend: `PublicTenantController GET /api/v1/public/tenants/{tenantId}/storefront` — tenant adı/logo/açıklama; product + search servisleri tüketip enrich ediyor
- [x] Frontend: `/store/:tenantId` route + `StorePage.tsx` (banner, logo, ürün grid — ürünler search-service tenant filtresiyle gelir)
- [x] Frontend: ürün kartlarındaki tenant adı bu sayfaya link veriyor
- [x] **Runtime verify (kullanıcı):** ✅ ürün kartından `/store/{tenantId}`'a git → banner + tenant ürünleri yüklensin

### FB-11: UI/UX genel iyileştirme ✅ 2026-06-10
**Kapsam:** Tüm frontend — tema sistemi + müşteri ekranları + account + merchant. Trendyol/Hepsiburada ayarında.
**Karar:** Turuncu e-ticaret paleti (`#F27A1A` + `#1A2238`), Inter font, fazlı (tema → müşteri → account+merchant).
- [x] **Tema sistemi**: `utils/themeTokens.ts` (yeni — renk/gölge/radius/gradient token'ları), `customTheme.ts` yeniden yazıldı (turuncu palet, Inter tipografi hiyerarşisi, component override'ları, `lighter`/`darker` augmentation), `index.html` Inter font
- [x] **Shared component**: `EmptyState.tsx`, `ProductCardSkeleton.tsx` (+ `ProductGridSkeleton`); 6 boş stub dosyası silindi
- [x] Ana sayfa: yeni hero (gradyan), **gerçek kategorilere bağlı** kategori kartları, güven şeridi, skeleton, bozuk "mobil uygulama" metni → kampanya bandı
- [x] Ürün listesi: tek dropdown → **sol filtre paneli** (kategori ağacı + fiyat aralığı + stok + sıralama), mobil filtre drawer, skeleton + EmptyState, URL `categoryId` desteği
- [x] ProductCard: `React.memo`, yumuşak gölge + hover lift, fiyat hiyerarşisi (radius/orantı kullanıcı geri bildirimiyle düzeltildi)
- [x] Ürün detay / sepet / mağaza: EmptyState, skeleton, tema renkleri; StorePage premium banner
- [x] **Account avatar bug FIX**: `AccountLayout` `userProfile` → `user`+`oidcProfile`; isim/email doluyor, `profileImageUrl` ile gerçek avatar. Header "Hesabım" butonuna da avatar
- [x] Merchant: `MerchantLayout` hardcoded renkler → tema token'ları (sidebar lacivert, vurgu turuncu)
- **Runtime:** Kullanıcı build aldı, görsel onayladı ("muhteşem"). Kart radius/fiyat orantısı ikinci turda düzeltildi.

### FB-12: Frontend performans iyileştirmesi — kod tarafı ✅ 2026-06-10
**Sorun:** Arama sayfası açılışı ~4s, genel yavaşlık gözlemlendi.
**Teşhis düzeltmesi:** Keşifte "obje query key → cache miss → 4s" denmişti; YANLIŞ — TanStack Query query key'leri structural hashler, cache hit olur. Gerçek darboğazlar: (1) filtre/sayfa değişiminde skeleton flicker, (2) App tüm-store aboneliği → tüm ağaç re-render, (3) icons tek mui-vendor chunk'ında.
- [x] **keepPreviousData**: `useSearchProducts`'a `placeholderData: keepPreviousData`; ProductListPage'de skeleton sadece ilk açılışta, sonraki fetch'te grid korunur (hafif solar) — flicker bitti
- [x] React Query `gcTime: 10dk` explicit (`main.tsx`)
- [x] **Zustand selector temizliği**: `App.tsx` (kritik — route ağacının tepesi), `CartPage`, `HomePage`, `ProductListPage` → tüm-store destructure yerine alan bazlı selector (gereksiz re-render kalktı)
- [x] **Referans stabilizasyonu**: ProductListPage `collectCategoryIds` + search payload `useMemo`; HomePage sabit payload modül seviyesine; StorePage payload `useMemo`
- [x] **Bundle**: `vite.config.ts` `@mui/icons-material` ayrı `mui-icons-vendor` chunk'ına; `rollup-plugin-visualizer` eklendi (`dist/stats.html`)
- [x] ProductCard `React.memo` (FB-11'de yapıldı)
- [x] **Runtime verify (kullanıcı):** ✅ `npm install` (yeni devDep) → `npm run build` → `dist/stats.html` chunk kıyas + flicker/re-render gözle doğrula
- **Sonraki tur (kapsam dışı bırakıldı):** `OrderHistoryPage` mock + yapay 2sn delay temizliği, redundant `useGetCategories()`, Header `React.memo`. search-service ES sorgu süresi (`took`) ölçümü backend tarafı.
- **Not:** 25 container aynı makinede — yavaşlığın bir kısmı ortamsal (cold ES sorgusu dahil).

---

## 🔧 Akış bütünlüğü / eksik temel işlevler (2026-06-10 servis taraması)

Tüm controller endpoint'leri tarandı; CRUD asimetrileri ve eksik temel akışlar. **Not:** stok ekle/kaldır (`manual-add`/`manual-remove`), sepet, sipariş, adres, ürün CRUD **simetrik ve tam** — sorun yok.

### FW-1: Depo (warehouse) tam CRUD + yaşam döngüsü ✅ 2026-06-11 (kod tarafı)
Salt update/delete değil, warehouse eksiksiz kaynak haline getirildi.
- [x] `GET /warehouses/{id}` (tekil detay), `PUT /warehouses/{id}` (ad+lokasyon; code immutable), `PATCH /warehouses/{id}/status` (aktif/pasif soft), `DELETE /warehouses/{id}` (boşsa kalıcı sil, stok varsa **409**)
- [x] 409 için lokal `WarehouseNotEmptyException` + `StockExceptionHandler` mapping; not-found `ResourceNotFoundException`→404
- [x] `StockRepository.existsByTenantIdAndWarehouseId` (delete ön kontrolü)
- [x] Pasif depoya stok eklemeyi engelle (`addManualStock` `WAREHOUSE_INACTIVE` guard)
- [x] Frontend `MerchantWarehousePage`: düzenle/sil dialog, aktif-pasif Switch+chip, 409 toast, pasif depoda "Stok Gir" disabled; `useUpdateWarehouse`/`useSetWarehouseStatus`/`useDeleteWarehouse` hooks (idempotency-key)
- [x] **Runtime verify (kullanıcı):** ✅ build (stock-service) + frontend; düzenle/pasif/sil + stoklu depo 409 testi

### FW-2: Mağaza duraklatma & kapatma akışı ✅ 2026-06-11 (kod tarafı)
Owner-only yaşam döngüsü + tam satış etkisi (ürünler gerçekten satıştan kalkar).
- [x] `TenantStateService` pause/resume/close (guard'lı: ACTIVE→PASSIVE, PASSIVE→ACTIVE, *→CLOSED terminal) + `TenantLifecycleService` orchestration; `TenantController` `POST /{id}/pause|resume|close` (OWNER)
- [x] Tek event: `TENANT_STATUS_CHANGED_EVENT` (`TenantStatusChangedEventPayload`, additive) → outbox
- [x] Authz: `findMemberRole` `status != 'CLOSED'` filtresi + close'ta tüm üyelerin `evictUserCache` + storefront cache evict
- [x] Tam satış etkisi: **search-service** `TenantEventConsumer` → `updateByQuery` `tenantActive` flip (arama zaten `tenantActive=true` filtreliyor); **product-service** `validateAndGetProduct` storefront status guard → `STORE_NOT_AVAILABLE` (checkout integrity)
- [x] mail-service: `TENANT_STATUS_CHANGED` case + handler + 3 şablon (paused/closed/reactivated)
- [x] Frontend: `StoreLifecycleSection` (MerchantSettings "Tehlikeli Bölge" — duraklat/geri aç + KAPAT onaylı dialog), `StorePage` kapalı mağaza durumu, `pauseTenant/resumeTenant/closeTenant` service
- **iyzico:** yapılacak iş yok (SDK 2.0.140 submerchant disable/delete desteklemiyor)
- **Kapsam dışı bırakıldı:** Admin `SUSPENDED` akışı (platform-admin auth altyapısı yok — ayrı iş)
- [x] **Runtime verify (kullanıcı):** ✅ build (event-contracts→common-lib→UTS, product, search, mail) → mağaza duraklat → ürünler aramadan/storefront'tan düşsün, checkout `STORE_NOT_AVAILABLE`, Mailhog'da mail; resume → geri gelsin; close → owner erişimi kesilsin

### FW-3: MinIO görsel orphan — silme entegrasyonu ✅ 2026-06-11
- [x] `ImageService.deleteImage(url)` + `deleteImages(collection)` eklendi (product + UTS). Best-effort: hata fırlatmaz, loglar; URL host-agnostik parse (eski `http://minio:9000/...` kayıtları da silinir).
- [x] Wiring:
  - **product-service** — `updateProduct` (yeni listede/main'de olmayan eski görseller), `deleteProduct` (tüm görseller), `ReviewServiceImpl.deleteReview` (yorum görselleri).
  - **user-tenant-service** — `TenantProfileService.uploadLogo` (eski logo), `UserService.updateProfileImage` (eski profil foto; controller'dan servise taşındı), `TenantStateService.closeTenant` (logo), `UserService.deleteUser` (profil foto).
- Tasarım: silmeler save'den SONRA best-effort; daha sıkı istenirse `TransactionSynchronization.afterCommit`.
- [x] **Multi-image upload bug FIX** ✅ 2026-06-12 — Frontend `MultiImageUpload` (`ImageUploadField.tsx`) bidirectional-sync feedback loop'u yüzünden ek görseller asla kalıcı olmuyordu (her ürün tek fotoda kalıyordu). İç `items` state + iki `useEffect` (parent↔child mirror) kaldırıldı; bileşen tam kontrollü yapıldı (`valuesRef` ile stale-closure'sız). Detay sayfasına (`ProductDetailPage.tsx`) tıklanabilir lightbox (ileri/geri + thumbnail) eklendi. Multi-foto + galeri artık uçtan uca çalışıyor. Backend zaten destekliyordu (değişmedi).
- [ ] **Kalan (opsiyonel):** tenant-bazlı key-prefix izolasyonu hâlâ ayrı borç (TECHNICAL-DEBT "MinIO veri izolasyonu").

### FW-4: Kategori yönetimi (admin CRUD) yok — incelenecek 🟢
- [x] **Zengin kategori seed eklendi** ✅ 2026-06-11 — `V7__seed_categories.sql` (product-service): Trendyol-vari 10 ana + ~55 alt kategori (2 seviye, slug+full_path+level), `ON CONFLICT (slug) DO NOTHING` ile idempotent.
- [ ] `CategoryController` sadece public read (`GET` list + slug). Dinamik kategori/admin paneli için `POST/PUT/DELETE` gerekir → **FW-6 Süper Admin Panel** kapsamında ele alınacak.

### FW-5: SubMerchant silme yok — değerlendirilecek 🟢
- [ ] `SubMerchantController` create + update var, delete yok. iyzico alt üye işyeri kaldırma gerekli mi (mağaza kapatma akışıyla — FW-2 — bağlantılı) değerlendirilmeli. **Not:** iyzico marketplace modelinde submerchant bağımsız ödeme almaz (para sadece platform split-payment başlatınca akar) + SDK 2.0.140 disable/delete sunmuyor → mağaza kapatınca iyzico tarafı işlem GEREKMİYOR. Açık kalan tek konu: CLOSED'da tenant'ın **platform aboneliğinin** yenileme tahsilatı durmalı (payment-service subscription, submerchant değil).

### FW-6: Süper Admin (Platform Yönetim) Paneli 🟠 — Sprint 1 ✅ 2026-06-13 (kod tarafı)
Platformun tamamını yöneten ayrı admin arayüzü + backend yetkilendirme katmanı. **Sprint 1 = auth temeli + mağaza yönetimi + kategori CRUD.**
- [x] **Platform-admin auth altyapısı** — Keycloak `platform-admin` client rolü; `JwtAuthConverter` zaten `ROLE_`'e map ediyor → `@PreAuthorize("hasRole('platform-admin')")`. `/users/me` artık `isPlatformAdmin` döndürüyor (JWT authority'den türetilir). **Korumasız `/public/admin/*` kapatıldı:** reindex → `/api/v1/products/admin/reindex`, resync → `/api/v1/stocks/admin/resync`, ikisi de `@PreAuthorize`'lı. (Mevcut gateway prefix'leri + `anyRequest().authenticated()` kullanıldı → gateway yml ve common-lib `GlobalSecurityConfig` değişmedi.)
- [x] **Tüm mağazaları yönet** — `AdminTenantController` `/api/v1/tenants/admin/stores` (sayfalı + status/q filtre), detay, **suspend/reactivate** (`TenantStateService.suspendTenant/reactivateTenant`, `TENANT_STATUS_CHANGED` event). search/product/storefront SUSPENDED'ı zaten inaktif sayıyor (`"ACTIVE".equals(status)`); mail-service'e SUSPENDED case + `tenant-suspended.html` eklendi.
- [x] **Kategori yönetimi (CRUD)** — `AdminCategoryController` `/api/v1/products/admin/categories` (GET tam ağaç/inaktif dahil, POST/PUT/DELETE/PATCH status). slug otomatik, level/fullPath parent'tan türetilir, cache evict, dolu kategori silme **409** (`CategoryInUseException` + `CategoryExceptionHandler`). Migration gerekmedi (kolonlar mevcut).
- [x] **Frontend** — `PlatformAdminProtectedRoute` + `AdminLayout` + `AdminStoresPage` (tablo/filtre/suspend-reactivate/detay) + `AdminCategoriesPage` (ağaç + ekle/düzenle/sil + aktif switch). `adminService` + `useAdminQueries` (idempotency-key). `/admin` route grubu, Header'da admin linki. tsc + eslint temiz.
- [x] **Runtime verify (kullanıcı):** ✅ Keycloak'ta `platform-admin` client rolü oluştur + kendine ata → yeniden login. Build sırası: UTS, product, stock, search, mail → frontend. Test: rolsüz `/tenants/admin/stores` 403; rollü 200; token'sız `/products/admin/reindex` 401/403; mağaza suspend → ürünler aramadan/storefront'tan düşsün + checkout `STORE_NOT_AVAILABLE`; kategori ekle → public `/categories`'de görünsün; dolu kategori sil → 409.
#### FW-6 Sprint 2 ✅ 2026-06-13 (kod tarafı) — siparişler + kullanıcılar + bug fix + kategori foto
- [x] **BUG FIX:** `searchForAdmin` `lower(bytea)` 500 — null `:q` Postgres'te bytea'ya bind oluyordu. LIKE deseni artık Java'da kuruluyor (`%q%` lowercase, String param). Aynı pattern user/order aramalarında da kullanıldı.
- [x] **Kategori görseli** — `POST /api/v1/products/admin/categories/{id}/image` (MinIO `categories/` klasörü, `ImageService` reuse, eski görsel best-effort sil, cache evict). `image_url` kolonu zaten vardı → migration yok. Frontend: kategori düzenle dialog'unda görsel yükleme (edit modunda; endpoint id gerektiriyor).
- [x] **Siparişler (admin)** — `AdminOrderController` `/api/v1/orders/admin/orders` (sayfalı + status/tenantId filtre), `/orders/{id}` detay (item'lar), `/admin/stats` (status dağılımı + GMV). Frontend `AdminOrdersPage`: tablo+filtre+detay modal + **@mui/x-charts** BarChart (status dağılımı) + GMV/sipariş kartları.
- [x] **Kullanıcılar (admin)** — `AdminUserController` `/api/v1/users/admin/users` (sayfalı + q/active filtre), `PATCH /{id}/status` (isActive toggle). Frontend `AdminUsersPage`: tablo+arama+aktif/pasif switch. **Sınır:** sadece UTS `isActive` flag'i; gerçek Keycloak login-block TODO (aşağı).
- [x] **@mui/x-charts** kuruldu. AdminLayout nav'a Siparişler+Kullanıcılar. tsc + eslint temiz. Gateway/migration/common-lib değişmedi.
- [x] **Runtime verify (kullanıcı):** ✅ build UTS+order+product → frontend (`npm install` + build). `/admin/orders` + `/admin/users` çalışsın; mağaza arama 500 gitmiş olsun; kategori foto yüklensin.

#### FW-6 Sprint 3 ✅ 2026-06-13 (kod tarafı) — gelir + transactions + merchant analitik
- [x] **Admin Genel Bakış + Gelir dashboard** — payment-service `AdminPaymentController`/`AdminPaymentService` `/api/v1/payments/admin/stats` (gelir özeti: PRODUCT_ORDER komisyonu + SUBSCRIPTION amount, SUCCESS filtreli + aylık seri `to_char(paidAt,'YYYY-MM')`). `PaymentRepository.revenueByType`/`monthlyRevenue`. product-service `AdminProductController` `/products/admin/stats` (`countByStatus`/`countByStatusNot`). Frontend boş `AdminDashboardPage` dolduruldu: 5 özet kartı (mağaza/ürün/sipariş/kullanıcı totalElements + toplam gelir) + komisyon/abonelik kartları + aylık gelir (stacked BarChart) + sipariş durum grafiği. AdminLayout'a "Genel Bakış" nav + `/admin` index artık overview.
- [x] **Transactions (ödemeler) admin listesi** — payment-service `PaymentRepository.searchForAdmin` (enum param null-safe) + `AdminPaymentController` `/api/v1/payments/admin/payments` (paged + type/status/tenantId filtre). Frontend `AdminTransactionsPage` (tablo+filtre) + "Ödemeler" nav + `/admin/transactions` route.
- [x] **Merchant analitik dashboard** — order-service `OrderItemRepository.topProductsByTenant`/`tenantTotalUnits` + `OrderRepository.tenantRevenue`/`tenantOrderCount` (iptal/iade hariç) + `MerchantAnalyticsService` + `OrderController` `GET /api/v1/orders/tenants/{tenantId}/analytics` (`@tenantSecurity.isMember`). Frontend `MerchantAnalyticsPage` (ciro/sipariş/adet kartları + top ürün horizontal BarChart + tablo) + MerchantLayout "Satış Analizi" nav + `/merchant/analytics`. **Gateway değişikliği gerektirmez (order zaten route'lu).**
- [x] **GATEWAY (devops-infra)** ✅ 2026-06-13 — payment-service route predicate'ine `/api/v1/payments/admin/**` eklendi (dar; `/internal/**`, `/process`, `/history/**` kapalı kaldı). `application-dev.yml` + `application-prod.yml`. **api-gateway restart gerekir.**
- [x] **Runtime verify (kullanıcı):** ✅ gateway route eklendikten sonra build payment+product+order+gateway → frontend. `/admin` overview dolu, `/admin/transactions` filtre çalışıyor, `/merchant/analytics` top ürünler; başka tenant analytics → 403.

#### FW-6 Sprint 3 düzeltmeleri ✅ 2026-06-14 (kullanıcı geri bildirimi)
- [x] **Merchant net kazanç** — komisyon checkout SAGA'da order'a taşındı: `InternalPaymentResponse`+`PaymentResult`+`commissionAmount` (additive), `orders.commission_amount` (migration **V3**), `OrderPersistence` set. `OrderRepository.tenantCommission` + `MerchantAnalyticsResponse.totalCommission/totalNet`. Frontend kartlar: Brüt Ciro / Platform Komisyonu / Net Kazanç. Eski sipariş `commission_amount` NULL → COALESCE.
- [x] **Transaction zenginleştirme** — `payments.buyer_email/buyer_name` denormalize (migration **V10**, checkout `BuyerInfo`'sundan set). Mağaza adı: UTS yeni internal `GET /api/v1/internal/tenants/names?ids=` + payment Feign (`UserTenantAuthzClient.getTenantNames`) → `AdminPaymentService` sayfa başına **tek** çağrıyla map'ler. `AdminPaymentSummaryResponse` zenginleşti (buyerEmail/Name, tenantName, iyzicoTransactionId, netAmount, refundedAmount, commissionRate). Frontend `AdminTransactionsPage`: Alıcı/Mağaza/Açıklama kolonları + detay modal.
- [x] **Grafik düzeltme** — merchant top-ürün grafiği dikey + `formatPrice` eksen formatı + uzun ad kısaltma.

#### FW-6 Sprint 3 kalan (hâlâ ertelendi)
- [ ] **iyzico GET settlement/payout entegrasyonu** — gerçek para/transaction verisi çekip saklama; harici API, büyük, ayrı iş.
- [ ] **User ban via Keycloak Admin API** — gerçek login-block (`keycloak-admin-client` yeni dependency gerekir; Sprint 2 UTS `isActive` flag'i ile sınırlı kaldı).

### FW-7: Ürün varyant sistemi (beden/renk/numara + varyant-bazlı stok) 🟠 — planlandı 2026-06-13
Klasik e-ticaret varyant seçimi (ayakkabı numarası, kıyafet bedeni, renk). Büyük, çok-servisli — **kendi sprint'i**. `parentProductId` entity'de var ama tamamlanmamış.
- Etkilenen servisler: **product-service** (varyant modeli + CRUD), **stock-service** (varyant-bazlı stok), **search-service** (renk/beden facet), **basket/order** (seçilen varyant id), **frontend** (detayda beden/renk seçici + merchant varyant/stok girişi).
- **Karar (uygulandı):** varyant = ayrı child-product (`parentProductId`). Stok child `productId` ile stock-service'te tutulur.
- **Tam kapsam ve servis-bazlı detay → `SERVICE-WORK.md` Product Service "Ürün varyant sistemi" maddesi.**

#### FW-7.1: Varyant UX revizyonu ✅ 2026-06-14 (kod tarafı)
Base varyant sistemi çalışıyordu ama UX iki yerde bozuktu: (1) müşteri stoksuz varyantı ancak checkout'ta öğreniyordu, (2) merchant varyantları tek tek (her biri elle SKU+fiyat+ayrı stok) ekliyordu.
- [x] **Müşteri — varyant-bazlı canlı stok.** Yeni public `POST /api/v1/public/stocks/availability` (`StockRepository.sumAvailableByProductIds` + `StockAvailabilityInfo`/`Response`; `availableQuantity` yalnız düşük stokta dolar, eşik=10 → envanter sızıntısı sınırlı). `GlobalSecurityConfig` `/api/v1/public/**` zaten permit; gateway route'a `/api/v1/public/stocks/**` eklendi (dev+prod). Frontend `useVariantStock` + `ProductDetailPage`: stoksuz varyant chip'i disabled+çarpılı+tooltip (diğer eksen seçimine duyarlı), "Son X adet kaldı!" uyarısı, miktar tavanı, add-to-cart seçili varyant stoğuna bağlı. Checkout reserve guard'ı korundu.
- [x] **Merchant — matris üretici + inline stok.** product-service `POST .../variants/batch` (`createVariantsBatch`, atomik, batch-içi + mevcut dedup). stock-service `POST .../manual-add/batch` (`addManualStockBatch`, tek tx; `addManualStock` private `doAddStock`'a ayrıldı → self-invocation yok). Frontend `MerchantVariantsModal` yeniden yazıldı: eksen editörü (chip-input) → "Kombinasyonları Üret" (kartezyen, mevcut kombinasyonları atlar) → tablo (SKU oto, fiyat parent'tan, "tümüne uygula", satır-içi stok) → depo seç → tek "Oluştur" (batch create → eşleştir → batch stok). Mevcut varyant tablosu + tek-varyant düzenleme korundu.
- **Kapsam dışı:** basket `validateAndGetProduct` stok-adedi kontrolü açılmadı (frontend gating + checkout reserve yeterli); varyantların ES'te ayrı döküman olması (search kirliliği) ayrı gözlem. event-contracts/migration/common-lib değişmedi.
- [x] **Runtime verify (kullanıcı):** ✅ build product-service + stock-service + api-gateway → frontend. Merchant: 2 eksen → üret → SKU oto/fiyat dolu/satır-içi stok → Oluştur (varyant+stok tek akış). Müşteri: stoksuz varyant çarpılı/disabled, düşük stokta "Son X adet", stoksuzda add-to-cart kapalı; `curl -XPOST localhost:8087/api/v1/public/stocks/availability -d '{"productIds":[...]}'`.

### FW-8: Admin/Merchant ürün yönetimi + sipariş→ürün navigasyonu + satış metrikleri 🟠 — planlandı 2026-06-13
Admin panelinin ürün tarafı + üç panelde sipariş→ürün gezinme + ürün-bazlı satış istatistikleri. FW-6 Sprint 3 / merchant analitik ile ilişkili.

- [x] **Admin ürünler sayfası** ✅ 2026-06-14 — `AdminProductController` `GET /api/v1/products/admin/products` (paged + q/tenantId/categoryId/status filtre, bytea-safe LIKE: desen Java'da kurulur, `:q` yalnız LIKE sağında). `AdminProductService.search` + `ProductRepository.searchForAdmin`. Frontend `AdminProductsPage` (tablo+filtre+görsel) + AdminLayout "Ürünler" nav + `/admin/products`.
- [x] **Admin ürün moderasyonu / kaldırma** ✅ 2026-06-14 — `PATCH /products/admin/products/{id}/deactivate` (→ INACTIVE) + `DELETE /products/admin/products/{id}` (soft delete → DELETED+OUT_OF_STOCK). `AdminProductService.deactivateProduct`/`removeProduct` mevcut `OutboxService.publishProductUpdatedEvent`/`publishProductDeletedEvent` + `ImageService.deleteImages` yolunu reuse eder (tenant guard yok) → ES senkronu event'le otomatik. Frontend satır aksiyonları + onay dialogu.
- [x] **Mağaza verify statüsü görünürlüğü** ✅ 2026-06-13 — `TenantSummaryResponse`'a `isVerified` eklendi + `TenantMapper` (`userTenantToSummary` explicit, `tenantToSummary` isimle otomatik). `AdminStoresPage` tabloya "Doğrulama" kolonu (Doğrulanmış/Doğrulanmamış chip); detay modalda zaten vardı. **Build: user-tenant-service + frontend.**
- [x] **Sipariş detayı → ürün detayı navigasyonu (3 panel)** ✅ 2026-06-14 — ürün adı `/product/:productId`'ye link. **Kullanıcı** `OrderDetailModal` (same-tab + modal kapanır), **Merchant** `MerchantOrderDetailModal` + **Admin** `AdminOrdersPage` detay modalı (yeni sekme, `target="_blank"` → panel context korunur). `OrderItem.productId` zaten vardı, backend değişikliği yok. (Hepsi public ProductDetailPage'e gider; ayrı admin ürün detay sayfası yok.)
- [x] **Ürün satış metrikleri (admin + merchant)** ✅ 2026-06-14 (kod tarafı) — ürün tablosundan açılan modal (📊): satılan adet + ciro + sipariş + **varyant kırılımı** (tablo + BarChart). **FW-7.1 nüansı:** satışlar varyant child id'sinde → parent metriği tüm varyant id'leri üzerinden toplanır. **Mimari (Option D):** order-service endpoint'leri sunar (`GET /tenants/{tid}/products/{pid}/metrics` @isMember + `GET /admin/products/{pid}/metrics` platform-admin); varyant id'lerini **mevcut order→product Feign** (`ProductServiceClient.getSalesIds`) ile çözer → yeni çevrimsel bağımlılık yok. product-service `GET /internal/products/{pid}/variant-ids` (`findVariantIdsByParentId`, DELETED dahil). order-service `OrderItemRepository.metricsByProductIds`/`distinctOrderCountByProductIds` (nullable tenant, status CONFIRMED/SHIPPED/DELIVERED) + `ProductMetricsService` + `ProductSalesMetricsResponse`. Frontend paylaşımlı `ProductMetricsModal` (MerchantProductsPage + AdminProductsPage). Gateway/migration/event-contracts değişmedi. tsc+eslint temiz. **Build: product-service + order-service → frontend. Bu FW-8'i kapatır.**

### FW-9: Refund / İade Sistemi 🟠 ✅ 2026-06-14 (kod tarafı)
İptal vardı ama refund **stub**'dı; teslim sonrası iade akışı yoktu. **Karar:** tam iade akışı (tüm sipariş) + merchant onay/admin override + **gerçek iyzico** (iptal=Cancel, iade=Refund).
- [x] **payment-service — gerçek iyzico:** `processRefund(orderId,amount,kind)` — `Cancel(paymentId)`/`Refund(paymentTransactionId,amount)` + Cancel→Refund fallback. `payment_transaction_id` ödeme anında yakalanır (**V11** migration). Endpoint `InternalRefundResponse{success,message,refundedAmount}` döner. (TECHNICAL-DEBT "Payment refund stub" kapandı.)
- [x] **order-service — iade workflow:** yeni `OrderStatus` RETURN_REQUESTED/RETURNED/RETURN_REJECTED (orders CHECK constraint **V4** ile güncellendi) + `Order` state metodları; `OrderReturn` entity/repo (**V4** `order_returns`); `OrderReturnService` (request/approve/reject + iyzico Refund, başarısızsa durum değişmez → çift refund yok); customer/merchant/admin endpoint'leri; mevcut iptal flow `kind=CANCEL`'a + compensation gerçek iyzico'ya çevrildi.
- [x] **stock-service — restock:** `ORDER_RETURNED` consumer → `restockForReturn` (satılmış stoğu aktif depoya geri ekler, `STOCK_STATUS_CHANGED`).
- [x] **event-contracts/EventConstants** (additive): 3 yeni ORDER event + payload. **mail-service:** 3 handler + 3 Thymeleaf şablon (return-requested/rejected/returned) + consumer case'leri.
- [x] **frontend:** `OrderStatus` union + status config'ler; müşteri `OrderDetailModal` "İade Talebi" (DELIVERED); yeni `MerchantReturnsPage` (Onayla/Reddet) + nav + route; admin `AdminOrdersPage` detay modalında override Onayla/Reddet. tsc+eslint temiz.
- **Gateway değişmedi; Debezium değişmedi** (yeni event tipleri mevcut ORDER connector'dan akar; `order_returns` outbox değil). **iyzico paneli işlemi gerekmez.**
- [x] **BUG FIX:** "Ödeme kaydı bulunamadı" — Pay-First SAGA'da `Payment.orderId` PRODUCT_ORDER'da NULL (ödeme sipariş persist'inden önce). Refund artık `findByIyzicoTransactionId(transactionId)` ile bulur (eski stub sessizce yutuyordu). `RefundRequest`/`InternalRefundRequest` 4 alan (orderId, transactionId, amount, kind).
- [x] **FW-9.1 iade kuralları** ✅ 2026-06-14 — **14 günlük pencere** (`Order.deliveredAt` V5, deliveredAt null→grace; frontend butonu gizler), **red TERMINAL** (RETURN_REJECTED'da kalır, DELIVERED'a dönmez → tekrar talep yok), **yapılandırılmış sebep** (`reasonCode` V5 + not; frontend radio seçenekleri, "Diğer" notu zorunlu). Migration **V5** (orders.delivered_at + order_returns.reason_code).
- [ ] **Runtime verify (kullanıcı):** build event-contracts→common-lib→payment,order,stock,mail→frontend (V11+V4+V5 migration'lar başlangıçta uygulanır, slot drop GEREKMEZ). İptal → iyzico Cancel logu + REFUNDED + mail. **YENİ** DELIVERED sipariş (deliveredAt dolu) → "İade Talebi" (sebep seç) → merchant "İadeler"de Onayla → iyzico Refund + stok geri (`curl .../stocks/tenant/{tid}`) + mail. Reddet → terminal, buton bir daha çıkmaz. 14 gün geçmiş siparişte buton "İade süresi doldu". Admin override.

---

## 📚 Dokümantasyon & Portfolio

### OpenAPI / Swagger UI ✅ 2026-06-07 (runtime doğrulandı)
- [x] Her Spring Boot servisine `springdoc-openapi-starter-webmvc-ui:2.8.13` eklendi (payment, product, search, basket, stock, order)
- [x] Her servise `OpenApiConfig.java` — Bearer JWT security scheme, contact bilgileri
- [x] UTS OpenApiConfig email güncellendi (`ilhan.kazan23@gmail.com`)
- [x] Tüm controller'lara `@Tag`, `@Operation`, `@ApiResponse` eklendi (~18 controller)
- [x] Internal/webhook endpoint'lere `@Hidden` (InternalPaymentController, InternalProductController, InternalStockController, InternalAuthzController, UserController sync metodları)
- [x] Public endpoint'lere `security = {}` (search, public product, categories, storefront)
- [x] Kritik DTO'lara `@Schema`: CheckoutRequest, PaymentRequest, ProductCreateRequest, ProductSearchRequest, CreateTenantRequest
- [x] Tüm servisler runtime'da test edildi — Bearer scheme + gruplar + hidden endpoint'ler doğrulandı
- Erişim: `http://localhost:808x/swagger-ui/index.html`

### Postman Collection (portfolio için kritik)
- [ ] Tüm platform için tek Postman collection oluştur (`IlhanKazan_ECommerce.postman_collection.json`)
- [ ] Environment değişkenleri: `{{baseUrl}}`, `{{token}}`, `{{tenantId}}`, `{{orderId}}` vb.
- [ ] Klasörler: Auth → Tenant → Product → Stock → Basket → Order → Search → Payment
- [ ] Her endpoint için örnek request body + beklenen response
- [ ] Pre-request script: token otomatik refresh
- [ ] GitHub'a commit et — README'de "Import this collection" bağlantısı ekle

### Platform Genel Dokümantasyon
- [ ] `ARCHITECTURE.md` güncel mi kontrol et — order-service akış diyagramı ekle
- [ ] `README.md` yaz (proje henüz README'siz) — proje tanıtımı, kurulum, servis listesi
- [ ] TÜBİTAK 2209-A başvurusu için teknik özet belgesi (ayrı bir `docs/` klasörüne)

---

## 🤖 AI Engine FastAPI (notu — proje sonunda)

Projenin **en son geliştirilecek servisi**. Detaylı feature ve mimari için `SERVICE-WORK.md` "AI Engine FastAPI" bölümüne bak.

**Şimdilik sadece not:**
- Stack: FastAPI + Pydantic + asyncpg + aiokafka + Redis + Transformers + LLM API
- Konum: `backend-ai/` (Java toolchain'den ayrı)
- Yeni event'ler gerekecek (additive): `PRODUCT_VIEWED_EVENT`, `REVIEW_CREATED_EVENT`, `RECOMMENDATION_FEEDBACK_EVENT`
- Geliştirilme sırası: sentiment → öneri → chatbot MVP → full chatbot → mağaza asistanı → tahmin
- Şu an yazma — Java servisler tracer bullet'tan geçince başla.

---

## 🎯 Doğrulama komutları

İş bitince doğrulamadan kapatma:

```bash
# Hızlı doğrulama: /verify-event-pipeline slash command
# Manuel:

curl -s http://localhost:8083/connectors | jq
curl -s http://localhost:8083/connectors/<name>/status | jq
docker exec -it kafka kafka-console-consumer --bootstrap-server kafka:9092 \
    --topic <TOPIC> --from-beginning --property print.headers=true --max-messages 3
curl -s localhost:9200/products/_count | jq
docker logs <service> 2>&1 | grep -iE 'migrat|flyway' | head
```

---

## ⚠️ Geçmiş tuzaklar (kısa hatırlatma — detay CONVENTIONS §16)

- Debezium `additional.placement` olmadan event sessizce kırılır
- `@Builder` vs `@SuperBuilder` parent + subclass uyumlu
- `JpaRepository<Inbox, Long>` bug — String olmalı
- Replication slot UUID→BIGINT geçişinde manuel drop
- `@Transactional` self-invocation proxy atlatmaz
- Hibernate lazy + `@Cacheable` entity = LazyInitializationException
- Build kullanıcı çalıştırır, ajan değil
- AI imza commit mesajına ekleme — `~/.claude/settings.json` `includeCoAuthoredBy: false`
- SAGA'da tüm zinciri tek `@Transactional`'a sarma — her adım kendi transaction'ında olmalı

---

## Update protokolü

- Yeni iş ekleme: kategoriye uygun yere — cross-cutting/debt → `TECHNICAL-DEBT.md`, servis-spesifik → `SERVICE-WORK.md`, aktif sprint → buraya
- Aktif iş bittiğinde: `- [x]` veya `✅ Tamamlanmış`a taşı
- Birden fazla bağımsız iş varsa Claude öncelik sıralayıp kullanıcıya sorsun, kendi başına seçmesin
- "Aktif"te 5'ten fazla item olmamalı — fazlaysa SERVICE-WORK'e geri taşı
