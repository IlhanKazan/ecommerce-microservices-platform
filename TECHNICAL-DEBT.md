# TECHNICAL-DEBT.md

Bu dosya bilinen technical debt'i kategori bazında listeler. **TODO.md'den farkı:** TODO aktif sprint odaklı; bu dosya genel borç envanteri. Bir item önceliklenip aktif iş olduğunda TODO.md "Aktif" bölümüne taşınır.

Kategori öncelik sırası: 🔴 kritik (güvenlik / veri kaybı) → 🟠 yüksek (production blocker) → 🟡 orta (kalite / DX) → 🟢 düşük (nice-to-have).

---

## 🔴 Güvenlik

### IDOR — payment-service ✅ Stage 10'da kapatıldı
- [x] `InternalPaymentController.processOrderPayment` — customerId artık `@CurrentUser`'dan alınıyor, body'den gelmiyor.
- ⚠️ Kısmi: `Payment.customerId` Long tipinde, `@CurrentUser.keycloakId()` UUID. Şimdilik `tenantId` geçiliyor. Gerçek fix: kolonu String/UUID'ye çevir (migration gerektirir) — TECHNICAL-DEBT olarak kalıyor.

### IDOR — stock-service ✅ Stage 10'da kapatıldı
- [x] Internal endpoint'lerde tenantId path'ten alınıyor, sadece o tenant'ın stoklarına erişilebiliyor (`tenantId + productId` filtreli sorgu).

### Kart bilgisi maskelenmesi — payment-service ✅ Stage 10'da kapatıldı
- [x] `PaymentServiceImpl.saveTransactionLogs` → `maskCardData()` helper eklendi. `cardNumber` son 4 hane, `cvc` `***` olarak loglanıyor.

### Fiyat manipülasyonu — order-service ✅ Stage 10'da çözüldü (tasarım kuralı uygulandı)
- [x] `OrderSagaServiceImpl`: fiyat product-service snapshot'tan hesaplanıyor, basket/frontend fiyatı kullanılmıyor. `OrderItem.unitPrice` = snapshot.

### Payment.customerId UUID→Long tip uyumsuzluğu 🟡
- [ ] PRODUCT_ORDER ödemelerinde `customerId` alanı şimdilik `tenantId` ile dolduruluyor (çünkü `@CurrentUser.keycloakId()` UUID, `Payment.customerId` Long).
- Çözüm: `payments` tablosunda `customer_id` kolonunu VARCHAR/UUID'ye çevir (Flyway migration + entity + repository değişikliği).
- Dosya: `InternalPaymentController`, `Payment.java`

### `/api/v1/internal/**` network koruması yok
- [ ] Mantıksal "internal" path'i ama network seviyesinde herhangi bir korumayla ayrılmamış. Servis hesabı role check veya k8s'te NetworkPolicy/mTLS gerekli (prod için).
- **Geçici çözüm (dev):** Internal endpoint'lere `hasRole('SERVICE')` ekle veya özel header kontrolü. Prod'da mTLS/NetworkPolicy.

### Concurrent sipariş — race condition ✅ 2026-06-07 düzeltildi
- [x] `StockExceptionHandler` eklendi (stock-service): `OptimisticLockingFailureException` → 409, `PessimisticLockingFailureException` → 409
- [x] order-service `reserveStockOrThrow()` mesajı güncellendi — her iki 409 kaynağını da doğru karşılıyor
- 500 stack trace yok, kullanıcıya anlamlı hata mesajı gidiyor

---

## Order Service — Bilinen Teknik Borç (Stage 10 sonrası)

### 🟡 Checkout idempotency gap ✅ 2026-06-07 düzeltildi
- [x] `IdempotencyAspect` catch bloğu güncellendi: `DataAccessException` / `TransactionException` durumunda Redis key silinmiyor (TTL: 5 dk). Diğer exception'larda (BusinessException, ExternalServiceException, FeignException) key silinir → retry allowed.
- Etki: `reserve ✅ → pay ✅ → DB ❌` → retry → `DUPLICATE_REQUEST` → double charge yok.

### 🟡 Payment refund stub — gerçek iyzico çağrısı yok
- [ ] `POST /api/v1/payments/internal/refund` şimdilik sadece `PaymentStatus.REFUNDED` set ediyor.
- Çözüm: `com.iyzipay.model.Cancel` ile iyzico refund API entegrasyonu.
- Dosya: `InternalPaymentController`, `PaymentServiceImpl`

### 🟡 shippingAddressJson parse edilmiyor
- [ ] `OrderSagaServiceImpl.parseShippingAddress` stub — `billingAddress` fallback kullanılıyor.
- Etki: Farklı shipping/billing adresi desteklenmiyor.
- Dosya: `OrderSagaServiceImpl.java`

### 🟢 Product snapshot N+1 Feign pattern
- [ ] Sepetteki her item için ayrı `GET /internal/products/{productId}/tenants/{tenantId}/validate` çağrısı.
- Çözüm: product-service'e batch endpoint ekle `POST /internal/products/batch-validate`.

### 🟢 STOCK_COMMIT_FAILED sonrası reservedQuantity serbest kalmıyor
- [ ] ORDER_CONFIRMED → async stok commit başarısız → order REFUNDED ama stok hâlâ `reservedQuantity`'de bekliyor.
- Çözüm: compensation sırasında stock-service'e rollback çağrısı ekle.

### 🟢 api-gateway ORDER route eksik
- [x] Stage 10.6'da eklendi — dev + prod yml güncel.

---

## 🟠 Production blocker

### iyzico SubMerchant komisyon mekanizması ✅ 2026-06-07 düzeltildi

**Yapılanlar:**
- `subscription_plans` tablosuna `commission_rate` eklendi (V5 migration)
- 4 mock plan seed edildi: Başlangıç %8 / Büyüme %5 / İşletme %3 / Kurumsal %1
- `TenantSubscription`'a `commission_rate` snapshot alanı eklendi
- `PaymentServiceImpl.resolveCommissionRate()` → aktif subscription'dan oran okunuyor, yoksa %8 fallback
- `ProductPaymentStrategy`: `subMerchantPrice = amount × (1 - rate/100)` — artık doğru hesaplıyor
- `Payment.commissionRate` ve `Payment.commissionAmount` artık kayıt altına alınıyor

**Kalan küçük borç:**
- [x] `fetchSubMerchantKeyOrThrow` (OrderSagaServiceImpl) — null/blank dönünce `SUBMERCHANT_NOT_READY` 400 fırlatılıyor, sessizce platforma düşmüyor. 2026-06-07
- [ ] Abonelik yükseltme/düşürme akışında commission_rate güncellenmeli (şimdi sadece ilk subscription'da set ediliyor)

### ORDER_DELIVERED event ve maili eksik ✅ 2026-06-07 düzeltildi
- [x] `EVENT_ORDER_DELIVERED` EventConstants'a eklendi, `OrderDeliveredEventPayload` event-contracts'a eklendi
- [x] `OrderStatusServiceImpl` DELIVERED case'inde outbox'a yazıyor
- [x] `MailEventConsumer` ORDER consumer'da handler'a yönlendiriyor, `order-delivered.html` şablonu eklendi

### Mail genişletme backlog (🟢 nice-to-have)
- [ ] **Şifre sıfırlama maili** — Keycloak kendi gönderiyor ama özel şablon yok (Keycloak SMTP'si Mailhog'a bağlı)
- [ ] **Hoş geldiniz maili** — yeni kullanıcı kaydında; Keycloak bu görevi üstleniyor, platform'dan ikinci mail atmak yerine Keycloak şablonu özelleştirilebilir
- [x] **Abonelik yenileme başarısız** ✅ 2026-06-07 — `SubscriptionRenewalProcessor.handlePaymentFailure` outbox yazıyor, mail-service PAYMENT topic'ten dinliyor
- [x] **Abonelik yenileme başarılı** ✅ 2026-06-07 — aynı
- [x] **Abonelik aktivasyon maili** ✅ 2026-06-07 — SUBSCRIPTION_ACTIVATED event'i mail-service'te wire edildi, `subscription-activated.html` eklendi
- [ ] **Merchant yeni sipariş bildirimi** — alıcıya değil, mağazaya bildirim (farklı mail adresi gerekli)



### Outbox cleanup scheduler eksik
- [ ] `payment-service` ve `basket-service` outbox cleanup scheduler **YOK**. Outbox tabloları sonsuz şişer.
- `product-service`, `stock-service`, `user-tenant-service`'te var (bkz. `OutboxRepository.deleteByCreatedAtBefore`).
- Çözüm: Her servise `@Scheduled(cron = "0 0 3 * * *")` + `deleteByCreatedAtBefore(LocalDateTime.now().minusDays(30))` ekle.

### Resilience4j eksik
- [ ] Şu an sadece `user-tenant-service → payment-service` çağrısı için circuit breaker var.
- Eksikler:
  - `stock-service → product-service` Feign çağrısı
  - `basket-service → product-service` Feign çağrısı
  - Genel olarak fallback eksik
- `PaymentServiceFallback` sınıfı **yorum satırında** + tip hatası var (String yerine SubMerchantResponse dönüyor).

### `.env.example` eksik
- [x] **Tamamlandı:** Kullanıcı tam içeriği gönderdi, devops-infra ajanı bu listeyle dosyayı güncelleyecek (TODO Stage'inde planla).

### docker-compose'da servisler eksik
- [x] **Tamamlandı Stage 9:** Tüm 8 servis + mail-service compose'a eklendi. Connector init `docker-init-connectors.sh`'e taşındı.

### Application YAML'da boşluklu key'ler
- [ ] Birkaç servisin `application.yml`'inde `circuit breaker`, `time limiter`, `resource server` (boşluklu) — `circuit-breaker`/`circuitbreaker`, `time-limiter`, `resource-server` olarak düzeltilmeli. Resilience4j binding'i kırılgan.

### DLQ (Dead Letter Queue)
- [ ] Kafka consumer'larında DLQ yok. Mesaj parse hatası, business exception'da mesaj sonsuz retry'a girebilir.
- Çözüm: Spring Kafka `ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer`.

### Servis-DB credentials izolasyonu yok
- [ ] Tüm DB'ler aynı `postgres` user kullanıyor. Bir servis compromise olursa diğerlerinin DB'sine de erişim. Prod için mutlaka her servise ayrı user.
- Notion'da: "Servis DBleri ayrı credentiallar"

### MinIO veri izolasyonu
- [ ] Tüm dosyalar aynı bucket'ta (`e-commerce-images`), tenant-bazlı izolasyon yok. Bir endpoint açığı varsa tüm dosyalar erişilebilir.
- Çözüm: Tenant-bazlı object key prefix (`tenants/<tenantId>/products/...`) veya tenant başına bucket.
- Notion'da iki yerde geçiyor: "DevOps" ve "user-tenant-service"

### MinIO public URL — container hostname sızıyor ✅ 2026-06-11 (çözüldü)
- [x] **Bug:** Logo/görsel yükleyince dönen URL `http://minio:9000/...` (container DNS) oluyordu; tarayıcı container-içi adrese erişemiyor, görsel kırık.
- **Kök neden:** `ImageService` görsel URL'ini **internal** MinIO endpoint'iyle (`minio.url`) kuruyordu. Internal (servis→MinIO upload) ile public (tarayıcı→MinIO indirme) endpoint ayrılmamıştı.
- **Çözüm (uygulandı):** Yeni `minio.public-url` config'i — `MinioClient` upload eskisi gibi `minio.url`'i (internal) kullanır; `ImageService`'in döndürdüğü URL `public-url` ile kurulur. dev/prod-local: `http://localhost:9005`; prod profilinde `${MINIO_PUBLIC_URL:...}`. Her iki servis (product + UTS) güncellendi.
- **Kalan:** Gerçek prod domeni için reverse proxy → FB-3.
- **Not (opsiyonel temizlik):** DB'de eski `http://minio:9000/...` yazılmış kayıtlar kırık kalır; dev verisi olduğu için yeniden upload ya da tek-seferlik `UPDATE ... REPLACE(url,'http://minio:9000','http://localhost:9005')`.

### MinIO güvenliği genel 🟠 (2026-06-11, kısmen çözüldü)
- [x] **Scoped service account** — `minio-init` container'ı (mc) bucket'a-özel policy'li `ecommerce-app` user'ı yaratır; uygulamalar artık root (`minioadmin`) DEĞİL bunu kullanır. Root yalnız server + console + init. (`infrastructure/minio/init.sh`, `app-policy.json`)
- [x] **Bucket policy explicit — ListBucket gerçekten kapalı** — `mc anonymous set download` preset'i anonim `s3:ListBucket` de veriyordu (enumerate fiilen AÇIKTI; önceki "kapalı" iddiası yanlıştı). Custom anonim policy (`mc anonymous set-json`, sadece `s3:GetObject` `/*`) ile düzeltildi: bilinen key ile obje GET açık, bucket listeleme `AccessDenied`. `curl` ile doğrulandı.
- [x] **Upload 500 regresyonu (scoped account'a geçişin yan etkisi) çözüldü** — MinIO Java SDK explicit region verilmeyince her işlemden önce `GetBucketLocation` çağırıyor; scoped policy'de o izin olmadığı için upload `AccessDenied` (500) veriyordu. Fix: `MinioConfig`'e `.region(${minio.region:us-east-1})` (yml'ye dokunmadan default) + `app-policy.json`'a `s3:GetBucketLocation` (defansif). Hem product hem UTS.
- [x] **Orphan silme** — bkz. `TODO.md` FW-3 (görsel değişimi/silimde `removeObject`).
- [ ] **Signed (presigned) URL** ile erişim — özel görseller (profil, doğrulama belgesi) için TTL'li imzalı URL gelecekte. Şu an tüm upload'lar public katalog görseli (ürün/logo/profil/review) → public-read kasıtlı.
- [ ] **Credentials yönetimi** — `.env`'deki MinIO key'leri prod'da secret manager'a; şu an scoped ama hâlâ statik `.env` credential.
- [ ] **Upload validation zaten var** (boyut/content-type) ama **virus/malware tarama** prod için gözden geçirilmeli.
- [ ] **Tenant key-prefix izolasyonu** — yukarıdaki "MinIO veri izolasyonu" maddesi (tek bucket, prefix yok).
- Bağlam: FB-3 + "MinIO veri izolasyonu" ile aynı küme.

### Frontend lint — CI blocker (42 error, 5 warning) ✅ 2026-06-10
- [x] **Çözüldü.** Asıl 42 error daha önce (Sprint 1 lint temizliğinde) giderilmiş; 2026-06-10'da kalan 3 warning de düzeltildi (`ProductListPage` categoryTree useMemo, `CartPage` useMemo deps). `npm run lint` → **0 error, 0 warning**. CI yeşil.
- _(Aşağıdaki eski hata envanteri tarihçe — üzerine dönme.)_
- **Dosyalar ve kategoriler:**
  - `@typescript-eslint/no-explicit-any` (30+ instance): `Header.tsx:50`, `HomePage.tsx:127`, `ProductDetailPage.tsx:124,441`, `CartPage.tsx:177`, `AddStockModal.tsx:54`, `TeamManagementSection.tsx:58`, `WarehouseManagement.tsx:56`, `CreateStorePage.tsx:192`, `MerchantSettings.tsx` (8 satır), `MerchantSubscription.tsx:136`, `MerchantWarehousePage.tsx:156,333`, `userService.ts` (5 satır), `AccountAddresses.tsx:89,109`, `useBasketQueries.ts:50,52,54`, `useProductQueries.ts:116,131`, `types/common.ts:7`
  - `@typescript-eslint/no-unused-vars` (5 instance): `KcPageLayout.tsx:2` (`Paper`), `Login.tsx:4` (`RouterLink`), `productService.ts:227` (`_payload`), `CreateStorePage.tsx:60` (`setActiveTenant`), `MerchantDashboard.tsx:75` (`prettyAddressType`), `main.tsx:43` (`_user`), `useBasketQueries.ts:5` (`IDEMPOTENCY_KEY_HEADER`)
  - `react-refresh/only-export-components` (1 instance): `NotificationProvider.tsx:51` — context export ayrı dosyaya taşınmalı
  - `react-hooks/exhaustive-deps` (5 warning): `App.tsx:82`, `ImageUploadField.tsx:46,153`, `CartPage.tsx:98`, `MerchantDashboard.tsx:44`
  - `@typescript-eslint/no-empty-object-type` (1 instance): `types/product.ts:197`
- **Çözüm:** `any` → `unknown` veya proper tip, kullanılmayan import'ları sil, `NotificationProvider`'da context export'u ayır, hook dep array'lerini düzelt.
- **Blokaj:** Her PR'da CI fail ediyor — frontend içersin içermesin lint job her seferinde çalışıyor.

---

## 🟡 Kalite / DX

### common-lib ağır transitive dependency sorunu
- [ ] `common-lib`'de `spring-cloud-starter-openfeign:5.0.0` hardcode edilmiş. Bu versiyon Jackson 3.x (`tools.jackson`) bekliyor; Spring Boot 3.5.x Jackson 2.x sağlıyor. Feign kullanmayan servisler (örn. `mail-service`) başlarken `FeignAutoConfiguration` patlamaya neden oluyor — şimdilik exclusion ile geçiştiriliyor.
- Kök sorun: `common-lib`'e güvenlik, Redis, Feign, AOP gibi tüm çapraz endişeler yığılmış. Bu yüzden hiçbir servis-spesifik utility (örn. `ImageService`, MinIO client) common-lib'e eklenemez — o servisi kullanmayan her servis de o bağımlılığı çeker.
- Çözüm (Parent POM ile birlikte ele alınmalı): common-lib'i `common-security`, `common-event`, `common-web` gibi odaklı modüllere böl; her servis sadece ihtiyacı olanı çeksin.
- Kısa vadeli: Feign ve Redis kullanmayan servislerde `common-lib` bağımlılığında exclusion eklendi (`mail-service`: `spring-cloud-starter-openfeign` + `spring-boot-starter-data-redis` exclude edildi).

### Inbox idempotency search-service'te kullanılmıyor
- [ ] Search-service event consume ediyor ama `BaseInbox` kullanmıyor. Aynı mesaj iki kere gelirse Elasticsearch'e iki kez yazılır (ES idempotent ama ES'in dışındaki yan etkiler — log'lar, metric'ler — duplicate olur).
- Çözüm seçenekleri:
  - (a) search-service'e minimal Postgres DB sadece inbox için
  - (b) Elasticsearch'te `processed_messages` index'i

### Outbox `@Transactional` self-invocation bug ✅ 2026-06-07 düzeltildi
- [x] `SubscriptionRenewalProcessor` yeni `@Service` bean'e taşındı. `processDailyRenewals` artık `renewalProcessor.processSingleRenewal(sub)` çağırıyor — Spring proxy devreye giriyor, `@Transactional` çalışıyor.

### Idempotency UTS createTenant'ta yok
- [ ] `TenantLifecycleService.createTenant` aynı kart ile iki kez POST = iki tenant. `@Idempotent` AOP eklenmeli.
- TODO [10.02.2026 11:21]

### createTenant catch-all yanlış event tetikliyor — ödeme başarılı ama başarısız maili gidiyor ✅ 2026-06-10
- [x] **UTS tarafı çözülmüş:** `TenantLifecycleService.createTenant` refactor edilmiş — `activateTenant` artık try bloğunun **dışında** (proc. sonrası), generic `catch(Exception e)` `markTenantAsPaymentFailed` ÇAĞIRMIYOR, sadece `TenantCreationException` fırlatıp tenant'ı PENDING'de bırakıyor. `PaymentResult.isInfrastructureError()/isSuccess()` ayrımıyla belirsiz durumda da PENDING. → Yanlış "ödeme başarısız" maili artık gitmez.
- [x] **İkinci senaryo (payment-service ghost-payment) çözüldü 2026-06-10:** iyzico success sonrası `createActiveSubscription`/outbox patlarsa para sessizce kaybolmasın diye `PaymentServiceImpl.handleIyzicoResponse` SUBSCRIPTION branch'i try-catch'e alındı. Patlayınca yeni `PaymentProvisioningFailureRecorder` (`@Transactional(REQUIRES_NEW)`, ayrı bean) `PROVISION_FAILED` statüsünde kalıcı bir "para izi" `Payment` kaydı yazar (iyzicoTransactionId dolu), sonra ana tx rollback'e izin verilir → çift provision olmaz, UTS tenant PENDING'de kalır. `PaymentStatus.PROVISION_FAILED` enum eklendi (string sütun, migration yok).
- **Kalan telafi borcu:** `PROVISION_FAILED` kayıtları için otomatik refund/retry scheduler yok — operatör manuel görmeli. Otomatik refund iyzico `Cancel` entegrasyonuna bağlı (bkz. "Payment refund stub").
- Dosya: `TenantLifecycleService.java`, `PaymentServiceImpl.handleIyzicoResponse`, `PaymentProvisioningFailureRecorder.java`

### Authz cache evict eksik durumlar
- [ ] Şu durumlar evict yapmıyor, kullanıcı eski rolüyle 2 saat takılı kalıyor:
  - createTenant sonrası ilk OWNER (cache yokken set edilmiyor)
  - Tenant SUSPENDED/CLOSED'a çekildiğinde
  - Tenant member silme

### Gateway + backend JWT double-decode
- [ ] Hem gateway'de hem backend'de JWT decode ediliyor. Birinde fail diğerinde geçer durumlar var. Tek noktada decode (genelde gateway) + backend trust'a geçilmeli.

### TenantContextHolder yok
- [ ] Şu an her metoda `Long tenantId` parametre olarak geçiliyor. ThreadLocal-based `TenantContextHolder` (zaten var olan `@CurrentUser` mantığında) eklenirse signature'lar daha temiz.
- Notion'da geçiyor.

### Cache: DTO yerine *Info recordu
- [x] **Tamam, pattern oturmuş.** CONVENTIONS.md'de detaylı.

### keycloak-spi tenantId payload'da yok
- [ ] Tenant izolasyonu için JWT payload'a `tenantId` enjekte etmek mantıklı olur. Şu an URL'den tenantId alınıp `TenantSecurityEvaluator` ile DB'den kontrol ediliyor — ekstra DB hit. JWT'de gelse cache'leme + cost düşer.

### PostgreSQL Row Level Security (RLS) — multi-tenant izolasyon katmanı
- [ ] Uygulama katmanı bug'larına karşı ek güvence olarak PostgreSQL RLS eklenebilir. Her tenant sadece kendi satırlarını görür; `tenantId` eşleşmezse DB query sonuç döndürmez.
- **Gerekli değişiklikler:**
  - Her `@Transactional` açılışında `SET LOCAL app.tenant_id = ?` çalıştıracak JDBC interceptor (Spring `TransactionSynchronizationManager` veya `DataSourceWrapper`)
  - Tenant-izole her tabloya Flyway migration'da `ALTER TABLE x ENABLE ROW LEVEL SECURITY; CREATE POLICY tenant_isolation ON x USING (tenant_id = current_setting('app.tenant_id')::bigint);`
  - Superuser/migration user policy'den muaf tutulmalı (`BYPASSRLS`)
  - Feign client'lar tenant context'i taşımıyor → iç servis çağrılarında RLS kırabileceği durumlar analiz edilmeli
- **Risk:** Mevcut `StockRepository` sorguları, Debezium CDC replication user'ı, ve inbox/outbox tabloları RLS ile etkileşime girebilir — kapsamlı test şart.
- **Öneri:** Order-service tamamlandıktan sonra, E2E testler yerleşince ele al.

### Frontend idempotency-key her isteğe
- [ ] Şu an sadece bazı endpoint'ler `@Idempotent`. Notion'da: "Frontendden her isteğe idempotency key header olarak gelecek". `@Idempotent` tüm POST/PUT'a yayılmalı.

### Rate limiting — ödeme ve sipariş endpoint'leri ⚠️ Stage 10 öncesi
- [ ] Gateway'de global rate limiting yok. Özellikle `POST /payments/process` ve (ileride) `POST /orders` endpoint'leri abuse'a açık.
- Çözüm: Redis + Bucket4j veya Spring Cloud Gateway `RequestRateLimiter` filter. Ödeme endpoint'i için daha sıkı (örn. kullanıcı başına dakikada 3 deneme).
- **Blokaj:** Order-service canlıya alınmadan önce ödeme + sipariş flow'unda rate limiting olmalı; aksi hâlde brute-force ve DoS riski.

### SonarQube sorunları
- [ ] Notion'da geçiyor — hangi sorunlar belirsiz. SonarQube/SonarLint çalıştırılıp çıkan kritik bulgular issue listesi haline getirilmeli.

---

## 🟢 Nice-to-have

### Parent POM
- [ ] Her servis ayrı parent-less, Spring Boot versiyonu hardcoded her pom.xml'de. Tek parent POM'da topla.

### Service discovery
- [ ] URL'ler env'den hardcoded. Eureka/Consul veya k8s service DNS.

### API versioning stratejisi
- [ ] `/api/v1/...` her yerde ama `/api/v2/...`'ye geçiş ne olacak belirsiz.

### OpenAPI/Swagger
- [ ] Sadece UTS'de Springdoc config var. Yayılmalı.


### Structured logging
- [ ] JSON format + correlation id + Loki entegrasyonu (Loki var ama log formatı plain).

### Kod tabanındaki TODO'ları takip
TODO yorumları (kod içi):
- `UserController.uploadProfileImage` — ImageService common-lib'e (Stage 6 ile ele alındı, üçüncü servis ekleme zamanı)
- `UserController.me` — Optional standardize
- `ProductPaymentStrategy` — order-service canlıya alındı, mock TODO kaldırıldı ✅
- `SubscriptionPaymentStrategy.calculatePrice` — planId doğrulaması
- `PaymentServiceClientAdapter.processPayment` — Feign hata detay analizi
- `TenantController` — CQRS değerlendirme (büyük iş, ileride)
- `TenantController.addMember` — Davet tablosu v2

### Config temizlik
- [ ] stock-service `application.yml`'inde `application.clients.user-tenant.url` var ama stock UTS'ye gitmiyor — gereksiz, sil.
- [ ] Frontend `.env` yorum satırları temizlenmeli
- [ ] `data/mockOrders.ts`, `data/mockProducts.ts` prod build'de exclude edilmeli (lint rule veya import guard)

### Test coverage genişlet (öncelik: kritik akışlar)
- [ ] `common-lib` — `IdempotencyAspect`, `TenantSecurityEvaluator`, `JwtAuthConverter`, `FeignClientInterceptor`, `GlobalExceptionHandler` (kritik yol, test yok)
- [ ] `keycloak-spi` — `UserServiceIntegration` (MockWebServer)
- [ ] `basket-service` — Redisson lock concurrent test, `ProductClientAdapter` WireMock, `Basket.addItem` unit
- [ ] `product-service` — `TenantProductServiceImpl` mutation + cache evict, `OutboxServiceImpl` JSON serialization
- [ ] `search-service` — `ProductEventConsumer` (`EmbeddedKafka` + Testcontainers ES)
- [ ] `user-tenant-service` — `TenantLifecycleService.createTenant` full flow, `AuthzCacheService`, `TenantMemberService`
- [ ] `payment-service` — `PaymentStrategy` dispatch, `SubscriptionRenewalService`, iyzico response handling

---

## ✅ Çözülmüş debt (tarihçe)

Üzerine dönme.

- BaseInbox/BaseOutbox kuruldu, tüm servisler kullanıyor (Stage 3, 4)
- `event-contracts` modülü kuruldu, payload kontratları orada
- Debezium connector'lara `additional.placement: message_type:header:message_type` eklendi
- search-service consumer header'dan event tipi okuyor (eskiden payload'dan okuyup bug üretiyordu)
- Stock-service Outbox UUID → BIGINT migration tamamlandı
- Inbox zenginleştirildi (status, retry, error, received_at)
- product-service ImageService Stage 6'da eklendi (UTS pattern'inden kopya)

---

## Update protokolü

- Yeni debt bulunduğunda: ilgili kategoriye ekle, başına `- [ ]` koy
- Çözüldüğünde: `- [x]` işaretle, eğer büyük bir item ise (mimari etkisi olan) "Çözülmüş debt" bölümüne taşı
- Item üzerinde aktif çalışma başladıysa **TODO.md "Aktif" bölümüne taşı**, debt'te `- [ ] (TODO.md Stage X.Y)` referansı bırak

Üzerine dönme — `- [x]` işaretli item'ları silme (tarihçe değerli).
