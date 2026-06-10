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

#### S2-2: Runtime verify (kullanıcı)
- [ ] Build al: event-contracts → common-lib → payment-service, order-service, mail-service
- [ ] `docker compose up -d --build payment-service order-service mail-service`
- [ ] Merchant bir siparişi "Teslim Edildi" yap → Mailhog'da order-delivered maili
- [ ] Yeni tenant oluştur → Mailhog'da hem tenant-activated hem subscription-activated (2 mail)
- [ ] Renewal test: cron geçici `0/30 * * * * *` yap → Mailhog'da renewal maili

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

### FB-9: Review sistemi (mock + entegrasyon)
**Sorun:** Ürün sayfalarında review/yorum bölümü yok.
- [ ] Backend: `review` entity + endpoint'leri (product-service'e ya da ayrı servis — karar verilecek)
  - `POST /products/{id}/reviews` — yorum ekle (auth gerekli)
  - `GET /products/{id}/reviews` — sayfalı liste (public)
  - Rating ortalaması `ProductDetailInfo`'ya ekle
- [ ] Backend: Geliştirme sırasında mock data üretici (seed script ya da endpoint)
- [ ] Frontend: ürün detay sayfasına yıldız rating + yorum listesi + yorum formu
- [ ] Frontend: Yorum gönderme mutation (React Query, idempotency key)

### FB-10: Public tenant tanıtım sayfası
**Sorun:** Tenant'ların müşteriye dönük public profil sayfası yok.
- [ ] Backend: `GET /public/tenants/{tenantId}` — tenant adı, logo, açıklama, kategori
- [ ] Backend: `GET /public/tenants/{tenantId}/products` — tenant'ın aktif ürünleri (sayfalı, filtreli)
- [ ] Frontend: `/store/{tenantId}` route — banner, logo, ürün grid
- [ ] Frontend: Ürün kartlarındaki tenant adı bu sayfaya link versin

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
- [ ] **Runtime verify (kullanıcı):** `npm install` (yeni devDep) → `npm run build` → `dist/stats.html` chunk kıyas + flicker/re-render gözle doğrula
- **Sonraki tur (kapsam dışı bırakıldı):** `OrderHistoryPage` mock + yapay 2sn delay temizliği, redundant `useGetCategories()`, Header `React.memo`. search-service ES sorgu süresi (`took`) ölçümü backend tarafı.
- **Not:** 25 container aynı makinede — yavaşlığın bir kısmı ortamsal (cold ES sorgusu dahil).

---

## 🔧 Akış bütünlüğü / eksik temel işlevler (2026-06-10 servis taraması)

Tüm controller endpoint'leri tarandı; CRUD asimetrileri ve eksik temel akışlar. **Not:** stok ekle/kaldır (`manual-add`/`manual-remove`), sepet, sipariş, adres, ürün CRUD **simetrik ve tam** — sorun yok.

### FW-1: Depo (warehouse) güncelleme & silme yok 🟠
- [ ] `WarehouseController` sadece `POST` (create) + `GET` (list). Depo açılıyor ama **düzenlenemiyor/silinemiyor**.
- [ ] Eklenecek: `PUT /warehouses/{id}` (ad/lokasyon güncelle), `DELETE /warehouses/{id}` (içinde stok yoksa sil — stok varsa 409).
- Dosya: `stock-service` `WarehouseController` + service/repo. Frontend: `MerchantWarehousePage`'e düzenle/sil aksiyonları.

### FW-2: Mağaza askıya alma & kapatma akışı yok 🟠
- [ ] `TenantStatus.SUSPENDED` / `CLOSED` enum'da var ama bu durumlara **geçiren hiçbir endpoint/iş akışı yok** (sadece repo query'de `status != 'CLOSED'` filtresi).
- [ ] Eklenecek: mağaza sahibi için "mağazayı kapat" (`CLOSED`), admin için "askıya al" (`SUSPENDED`) endpoint'leri + ilgili event/mail + authz cache evict (bkz. TECHNICAL-DEBT "Authz cache evict eksik").
- Dosya: `user-tenant-service` `TenantController` + `TenantStateService`.

### FW-3: MinIO görsel orphan — silme entegrasyonu yok 🟡
- [ ] Hiçbir serviste MinIO `removeObject` yok. Ürün/profil/tenant görseli **değiştirilince veya ürün silinince eski dosya MinIO'da kalıyor** (storage leak; zamanla şişer).
- [ ] Eklenecek: `ImageService.deleteImage(url)`; ürün update'te listeden çıkan görselleri, ürün delete'te tüm görselleri, profil/logo değişiminde eskisini sil. (Tenant-bazlı izolasyon borcuyla birlikte ele alınabilir.)
- Dosya: `ImageService` (product-service + user-tenant-service).

### FW-4: Kategori yönetimi (admin CRUD) yok — incelenecek 🟢
- [ ] `CategoryController` sadece public read (`GET` list + slug). Yeni kategori yalnızca Flyway seed ile ekleniyor; dinamik kategori/admin paneli için `POST/PUT/DELETE` gerekir. Admin paneli kapsama alınırsa iş, değilse kabul edilebilir.

### FW-5: SubMerchant silme yok — değerlendirilecek 🟢
- [ ] `SubMerchantController` create + update var, delete yok. iyzico alt üye işyeri kaldırma gerekli mi (mağaza kapatma akışıyla — FW-2 — bağlantılı) değerlendirilmeli.

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
