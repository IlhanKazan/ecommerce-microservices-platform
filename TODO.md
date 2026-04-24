# TODO.md

Bu dosya üç kaynaktan beslenir:
1. **Tamamlanmış aşamalar** (ne çalışıyor, üzerine dönme)
2. **Aktif ve sıradaki migration aşamaları** (şu an elinde olan iş)
3. **Kod tabanındaki teknik borç** (TODO yorumları + convention sapmaları)
4. **Frontend ↔ Backend arası açık talepler** (frontend'in beklediği backend değişiklikleri)
5. **Eksik testler** (servis bazında)
6. **İleride yapılacaklar** (v2, README'de vaat edilen planlı servisler)

Format: Her item kısa başlık + bağlam + (varsa) dosya referansı + zorluk (S/M/L/XL). Bittiği zaman satırın başına `- [x]` koy ya da taşı "Tamamlanmış" bölümüne.

---

## ✅ Tamamlanmış

Üzerine dönme, düzeltme yapmana gerek yok.

- **Stage 1: product-service event akışı** ÇALIŞIYOR.
  - Outbox → Debezium → Kafka → Elasticsearch zinciri end-to-end sağlam.
  - `common-lib/BaseOutbox` yerinde, product-service entity'si extend ediyor.
  - `V2__outbox_CDC_config.sql` `REPLICA IDENTITY FULL` ekledi.
  - Debezium `product-service-connector` doğru config ile çalışıyor (`message_type:header:message_type`).
- **Stage 2: search-service consumer `event-contracts`'a geçti.**
  - Eski inline consumer DTO'ları silindi.
  - `@Header("message_type")` kullanımı Debezium config'iyle tutarlı.
  - `ProductCreatedEventPayload`, `ProductUpdatedEventPayload`, `ProductDeletedEventPayload`, `StockStatusChangedEventPayload` event-contracts'tan import ediliyor.
- **common-lib geliştirmeleri:**
  - `BaseInbox` zenginleştirildi (`status`, `retry_count`, `error_message`, `received_at`, `processed_at`)
  - `InboxStatus` enum'u `com.ecommerce.common.constant` altına taşındı, tüm servisler import edecek.
  - `@Idempotent` AOP + `@CurrentUser` resolver + `TenantSecurityEvaluator` + `FeignClientInterceptor` stabil.
- **product-service Inbox** `BaseInbox` extend ediyor, `@SuperBuilder` annotation'ı düzeltildi ✅.
- **stock-service Inbox** `BaseInbox` extend ediyor ✅.
- **stock-service migration V3** DB tarafı (outbox drop+recreate as BIGINT IDENTITY, inbox enrich) tamamlandı ✅.
- **stock-service Java tarafı migration (Stage 3.3)** tamamlandı ✅:
  - `StockInfo` → `stock/query/` paketine taşındı, `implements Serializable` eklendi.
  - `Outbox.java`: `@GeneratedValue(IDENTITY)`, `@AllArgsConstructor` kaldırıldı, `@SuperBuilder` zaten vardı.
  - `InboxService` interface'e `isMessageProcessed` metodu eklendi, Impl'e `@Override` eklendi.
  - `V4__outbox_replica_identity_full.sql` eklendi.
  - `V5__add_inbox_indexes.sql` eklendi (idx_inbox_status, idx_inbox_pending partial index).
- **Stage 3: stock-service event pipeline** ÇALIŞIYOR ✅
  - 3.1: Tüm connector'lar `register_connector.sh` ile push edildi, `message_type` header'lı.
  - 3.2: stock-service replication slot drop edildi, connector re-register edildi, `RUNNING`.
  - 3.3: Java tarafı migration tamamlandı (yukarıda detay).
  - 3.4: End-to-end verify: `addManualStock` → outbox → Debezium → Kafka `STOCK_STATUS_CHANGED_EVENT` (message_type header'lı) → search-service consumer → Elasticsearch `inStock: true`. Yeni ürün ekleme de `PRODUCT_CREATED_EVENT` zinciriyle ES'e düştü.

---

---

## ⏭️ Sıradaki (Stage 4)

### 4.1 payment-service migration
- [x] Önce **oku**: `backend/payment-service/src/main/java/com/ecommerce/paymentservice/{outbox,inbox}/entity/*.java`. Mevcut tam tanımlı Outbox/Inbox var, `BaseOutbox`/`BaseInbox` extend etmiyor.
- [x] `payment-service/pom.xml`'e `event-contracts` dependency ekle.
- [x] `outbox/entity/Outbox.java` → `extends BaseOutbox`, kendi field'larını sil (`processed`, `sentAt` dahil — Debezium pattern'ında gereksiz).
- [x] `inbox/entity/Inbox.java` → `extends BaseInbox`, kendi inline `InboxStatus` enum'unu sil, `com.ecommerce.common.constant.InboxStatus` import et.
- [x] Bu iki entity'nin `@SuperBuilder` kullandığından emin ol.
- [x] `InboxRepository` oluşturuldu: `JpaRepository<Inbox, String>` (PK String).
- [x] `InboxService` + `InboxServiceImpl` oluşturuldu (stock-service pattern — `REQUIRES_NEW` + idempotency).
- [x] `OutboxService` arayüzüne `publishPaymentSuccessEvent`, `publishPaymentFailedEvent`, `publishSubscriptionActivatedEvent` eklendi; `OutboxServiceImpl` implement etti (`MANDATORY` propagation, ObjectMapper).
- [x] `PaymentServiceImpl`'e `OutboxService` inject edildi; `handleIyzicoResponse` SUCCESS/FAILURE branch'larına publish çağrıları eklendi.
- [x] `EventConstants`'a `AGGREGATE_PAYMENT`, `EVENT_PAYMENT_SUCCESS`, `EVENT_PAYMENT_FAILED`, `EVENT_SUBSCRIPTION_ACTIVATED` eklendi.
- [x] `event-contracts`'a `PaymentSuccessEventPayload`, `PaymentFailedEventPayload`, `SubscriptionActivatedEventPayload` eklendi (`com.ecommerce.contracts.event.payment.*`).
- [x] Flyway `V3__migrate_outbox_inbox_for_base_classes.sql` eklendi: `processed`/`sent_at` DROP, `retry_count` ADD, `REPLICA IDENTITY FULL`.
- [x] `infrastructure/debezium/payment-connector.json` oluşturuldu (`payment_db`, slot: `payment_service_debezium_slot`).
- [x] `register_connector.sh`'a `payment-connector.json` satırı eklendi.
- [ ] **Verify (runtime)**: payment-service restart → Flyway V3 migration uygulansın → `register_connector.sh` çalıştır → abonelik ödemesi yap → PAYMENT topic'inde mesaj gör.
- **M-L**

### 4.2 user-tenant-service migration
- [ ] Önce **oku**: `backend/user-tenant-service/src/main/java/com/ecommerce/usertenantservice/outbox/entity/Outbox.java`. Kendi tam tanımlı entity'si var.
- [ ] `user-tenant-service/pom.xml`'e `event-contracts` dependency ekle (publish edilecek event varsa).
- [ ] `Outbox` entity → `extends BaseOutbox`.
- [ ] `@SuperBuilder` uyumu.
- [ ] DB şeması — V1'de yaratılmış outbox tablosu BaseOutbox'la uyumlu mu, kolon adları (`message_type` vs. `event_type`) tutarlı mı kontrol et. Uyumsuzsa V4 migration.
- [ ] `REPLICA IDENTITY FULL` set.
- [ ] Debezium `user-tenant-connector.json` zaten var (`register_connector.sh`'tan çağrılıyor), push'unu verify et.
- [ ] Publish senaryosu: `TENANT_CREATED_EVENT`, `TENANT_ACTIVATED_EVENT`, `TENANT_SUSPENDED_EVENT`? Consumer tarafı şu an yok — sadece schema hazırlığı mı, yoksa gerçek consumer da mı yazılacak? Kullanıcıya danış.
- **M**

---

## 🧩 Frontend ↔ Backend arası açık talepler

Frontend chat'lerinden gelen, backend tarafında **henüz yapılmamış** istekler:

### FB-1: Ürün görseli upload endpoint'i
- [ ] product-service'e `POST /api/v1/products/images/upload` endpoint'i ekle.
- Multipart form-data `file` parametresi alır, backend proxy ile MinIO'ya yazar, public URL döner.
- UTS'deki `ImageService` (`user/service/ImageService.java`) implementasyonunu referans al — aynı pattern, `folderName = "products"`.
- common-lib'e taşımak daha temiz olur (UTS'deki TODO'da da bahsediliyor) ama şimdilik kopyala, refactor Stage 5+.
- Frontend `productService.uploadProductImage(file)` bu endpoint'i çağırıyor, dönen URL'i state'e yazıp submit'te product create/update request'inde `mainImageUrl` / `imageUrls` olarak yolluyor. Base64 YOK, sadece MinIO URL'leri.
- **M**

### FB-2: Tenant product detail endpoint'i
- [ ] Frontend edit formu açılınca `getTenantProductById(tenantId, productId)` çağırıyor (`useGetTenantProductById` hook). Mevcut endpoint'i incele:
  - `GET /api/v1/products/tenants/{tenantId}/{productId}` — `TenantProductController.getTenantProduct` → `ProductInfo` dönüyor.
  - `ProductInfo` **sınırlı alanlar** içeriyor: id, tenantId, categoryId, categoryName, parentProductId, name, sku, price, currency, mainImageUrl, status, salesStatus, attributes. **`description`, `imageUrls`, `brand`, `weightGrams`, `dimensionsCm`, `seoTitle/Description/Keywords`, `minOrderQty`, `maxOrderQty`, `tags`, `discountedPrice` YOK.**
  - Frontend edit formu full detay istiyor. Seçenek:
    - **(A)** Yeni `ProductDetailInfo` record'u yap, `TenantProductService.getProductDetailByIdAndTenantId` ekle, controller'da yeni endpoint (örn. `/api/v1/products/tenants/{tenantId}/{productId}/detail`) veya mevcut endpoint'i genişlet.
    - **(B)** `ProductInfo`'ya tüm field'ları ekle (breaking change, tüm kullanım noktalarını güncelle).
  - Önerim: (A) yolu temiz. Kullanıcıya hangi yolu seçeceğini sor.
- **M**

### FB-3: MinIO prod reverse proxy (not)
- Prod geçişte MinIO nginx reverse proxy arkasına alınmalı (signed URL + access control). Şu an dev'de doğrudan erişiliyor.
- Dev'de iş değil, prod roadmap'te not.

---

## 📋 Kod tabanındaki TODO yorumları

Kodda `// TODO [tarih HH:MM]: ...` formatında bırakılmış notlar. Tarih sırasıyla:

### Early (2025)
- `UserController.uploadProfileImage` — [11.12.2025 18:54] ImageService common jar'a taşı (profile/product/tenant ortak). FB-1 ile birleştir.
- `UserController.me` — [29.12.2025 06:48] Optional kullanımı standardize et.
- `PaymentController.processPayment` — [29.12.2025 00:33] `@CurrentUser` ile token doğrulaması ekle (şu an body'deki `customerId` güveniliyor, güvenlik açığı).

### Payment / Iyzico
- `PaymentServiceImpl.saveTransactionLogs` — [28.12.2025 06:04] **Kart bilgileri maskelenecek** (rawRequest string'i). Compliance için **öncelikli**. **M**
- `ProductPaymentStrategy` — tüm metotları mock. order-service devreye girince doldur. **L** (order-service yazılınca)
- `SubscriptionPaymentStrategy.calculatePrice` — [08.02.2026 22:28] planId doğrulaması
- `SubscriptionRenewalServiceImpl.processDailyRenewals` — [08.02.2026 22:30] `@Transactional` self-invocation bug'ı (`processSingleRenewal` aynı sınıfta, proxy'den geçmez → transaction açılmıyor). Çözüm: method'u ayrı bean'e al, veya `AopContext.currentProxy()`. **M**
- `PaymentServiceClientAdapter.processPayment` — [09.02.2026 23:54] Feign hata detay analizi
- `PaymentServiceFallback` — tüm dosya yorum satırında + tip hatası (String dönmeli SubMerchantResponse dönüyor). Düzelt ve aktifleştir — Resilience4j circuit breaker'ı için fallback şart. **M**

### UTS
- `TenantController` — [10.02.2026 11:33] CQRS değerlendirme
- `TenantController.addMember` — [06.02.2026 14:50] Davet tablosu v2
- `TenantLifecycleService.createTenant` — [10.02.2026 15:04] Yarım ödeme için retry endpoint'i (kısmen `retryTenantPayment` ile çözüldü, senaryolar kapalı mı teyit)
- `TenantLifecycleService.createTenant` — [10.02.2026 11:21] **Idempotency** (aynı kart ile iki POST → iki tenant). `@Idempotent` AOP ekle. **M**

### Diğer
- `basket-service ProductClientAdapter.validateAndGetProduct` — yorumdaki stok kontrol blok'u. Product response'a `stockQuantity` eklenince aç. **S**
- Birden fazla servisin `application.yml`'inde `resource server` (boşluklu) — `resource-server` olarak düzelt. **S**

---

## 🏗️ Mimari boşluklar (Stage'lerin dışında, genel)

### Event pipeline sağlamlığı
- [ ] **DLQ (Dead Letter Queue)** Kafka consumer'larında yok. Spring Kafka `ErrorHandlingDeserializer` + `DeadLetterPublishingRecoverer`. **M**
- [ ] **Consumer inbox idempotency** search-service'te kullanılmıyor. İki seçenek: (a) search-service'e minimal Postgres DB sadece inbox için, (b) Elasticsearch `processed_messages` index'i. Kullanıcıya danış. **L**
- [ ] **Outbox cleanup scheduler** payment-service'te YOK (product, stock'ta var). Outbox tablosu sonsuz şişer. **S**

### Auth/security
- [ ] **`/api/v1/internal/**` endpoint'leri network-level korunmuyor.** Servis hesabı / role check veya mTLS. **M**
- [ ] **authz cache evict edilmeyen durumlar**: createTenant sonrası ilk OWNER, tenant SUSPENDED/CLOSED. **S**
- [ ] **Gateway + backend JWT double-decode.** Tutarlı bir strateji seç (tek noktada decode). **M**

### Config
- [ ] **`.env.example` ÇOK EKSİK.** Dosyada 7 değişken, kodda 20+ kullanılıyor. Eklenecekler:
  ```
  REDIS_PASSWORD
  KEYCLOAK_CLIENT_ID
  REALM_NAME
  PAYMENT_DB_NAME / USERNAME / PASSWORD
  PRODUCT_DB_NAME / USERNAME / PASSWORD
  STOCK_DB_NAME / USERNAME / PASSWORD
  USER_DB_NAME / USERNAME / PASSWORD
  USER_TENANT_SERVICE_URL
  PRODUCT_SERVICE_URL
  MINIO_ACCESS_KEY / SECRET_KEY
  USER_MINIO_BUCKET
  IYZICO_API_KEY / SECRET_KEY / BASE_URL
  MY_SPI_KEYCLOAK_TOKEN_URL / CLIENT_ID / CLIENT_SECRET / USER_SERVICE_HEALTH_URL
  ```
  **S**
- [ ] stock-service `application.yml`'inde `application.clients.user-tenant.url` var ama stock hiç UTS'ye gitmiyor — gereksiz, temizle. **S**

### Resilience
- [ ] Resilience4j **sadece UTS → payment-service** için config'li. Diğer Feign çağrılarında yok. En azından şu linkler için ekle: stock → product, basket → product. **M**
- [ ] `PaymentServiceFallback` aktifleştir (yukarıda).

### Observability
- [ ] **Zipkin, Prometheus, Grafana** ana `docker-compose.yml`'de yok. `infrastructure/devops/docker-compose.yml` ayrı, merge et. **M**

### Frontend (notlar)
- [ ] `.env` yorum satırları temizlenmeli
- [ ] `data/mockOrders.ts`, `data/mockProducts.ts` prod'a sızmamalı — lint rule veya import guard

---

## 🧪 Eksik testler

### Hiç test yok
- `api-gateway` — routing smoke test
- `common-lib` — `IdempotencyAspect`, `TenantSecurityEvaluator`, `JwtAuthConverter`, `FeignClientInterceptor`, `GlobalExceptionHandler`. Kritik yol, test yok. **L**
- `event-contracts` — sadece record'lar, gerek minimal
- `keycloak-spi` — `UserServiceIntegration` HTTP mock (MockWebServer). **M**
- `basket-service` — `BasketService.addItemToBasket` concurrent (Redisson lock), `ProductClientAdapter` WireMock, `Basket.addItem` unit. **M**
- `product-service` — `TenantProductServiceImpl` mutation + cache evict, `ReviewServiceImpl` edge case'ler, `OutboxServiceImpl` JSON serialization, `CategoryServiceImpl.toInfo` recursive mapping. **L**
- `search-service` — `ProductEventConsumer` (`EmbeddedKafka` + Testcontainers ES), `ProductSearchServiceImpl` DSL query. **L**
- `user-tenant-service` — `TenantLifecycleService.createTenant` full flow (payment mock), `AuthzCacheService`, `TenantMemberService` rol geçişleri, `Address.validateOwnership` @PrePersist. **L**
- `payment-service` — `PaymentStrategy` dispatch, `SubscriptionRenewalService.processSingleRenewal`, iyzico response handling. **XL** — iyzico mock zor

### Test var ama eksik
- `stock-service`:
  - `StockControllerIdempotencyTest`, `StockControllerTest` — auth senaryoları (OWNER değil → 403) eksik
  - `StockServiceTest` — `reserveStockForOrder` full flow (outbox + movement) yok
  - `ProductClientAdapterIT` — downstream error edge case'leri ekle
  - End-to-end outbox → Debezium → Kafka → consumer test yok (Testcontainers ile yapılabilir ama overhead)

### Test altyapısı
- `AbstractBaseIntegrationTest` zaten var, extend et
- `@AutoConfigureWireMock(port = 0)` Feign test'leri için standart
- `@MockitoBean JwtDecoder` security bypass
- `application-test.yml` gerekliyse ekle (stock'ta var)

---

## 🚀 İleride yapılacaklar (README'de vaat edilen + v2)

### README'de vaat edilen planlı servisler
- [ ] **order-service (SAGA Orchestrator)** — **XL**
  - Saga adımları: Order Create → Stock Reserve → Payment Process → Order Confirm, her biri için compensating action.
  - Orchestration pattern (choreography değil). State machine (Spring StateMachine veya manuel).
  - `saga_orchestrator_db` hazır, `AGGREGATE_ORDER` sabiti var, `ProductPaymentStrategy` mock'u doldurulacak.
- [ ] **mail-service (Notification)** — **L**
  - Event-driven consumer: `PaymentFailed`, `PaymentSuccess`, `OrderShipped`, `TenantActivated` vb.
  - Mailhog dev'de hazır, prod SMTP config'lenebilir.
- [ ] **AI Engine (FastAPI)** — **XL**
  - İzole Python servisi.
  - NLP ürün yorum özetleme (`Product.aiReviewReport` field hazır).
  - Öneri sistemi (collaborative filtering? content-based?).
  - LLM-powered virtual assistant.
  - Main stack Java olduğu için Python tarafı ayrı build/deploy.

### E-ticaret akışının diğer eksikleri
- [ ] **Ürün varyantları** UI (`Product.parentProductId` altyapısı hazır)
- [ ] **Discount/Coupon** yönetim endpoint'i (`discount_percentage`, `discounted_price` kolonları var)
- [ ] **Review moderasyon akışı** — `PENDING → APPROVED` endpoint yok
- [ ] **Subscription upgrade/downgrade** + pro-rata
- [ ] **Kullanıcı sipariş geçmişi** (order-service gelince)
- [ ] **Mağaza davetiyesi** kabul akışı
- [ ] **Sepet stok doğrulaması** (basket-service yorumdaki blok)
- [ ] **i18n** (UI şu an Türkçe)

### Teknik iyileştirmeler
- [ ] **Parent POM** — her servis ayrı parent-less, versiyonlar hardcoded. Tek parent POM'a al. **M**
- [ ] **Service discovery** — URL'ler env'den hardcoded. Eureka/Consul/k8s. **L**
- [ ] **API versioning stratejisi** — `/api/v2/` nasıl olacak?
- [ ] **OpenAPI/Swagger** — sadece UTS'de config var, yayılacak. **M**
- [ ] **Rate limiting** — gateway'de yok. Redis + Bucket4j. **M**
- [ ] **Structured logging** — JSON format + correlation id + Loki

---

## 🎯 Doğrulama komutları

İş bitince "bitti" demeden önce en azından ilgili komutları çalıştır:

```bash
# Debezium connector listesi
curl -s http://localhost:8083/connectors | jq

# Connector aktif config'i — push edilen mi yoksa eski mi teyit
curl -s http://localhost:8083/connectors/<connector-name>/config | jq

# Connector status
curl -s http://localhost:8083/connectors/<connector-name>/status | jq

# Replication slotlar
docker exec -it postgres psql -U $POSTGRES_USER -c "SELECT * FROM pg_replication_slots;"

# Replication slot drop (migration sırasında)
docker exec -it postgres psql -U $POSTGRES_USER -d <db_name> \
  -c "SELECT pg_drop_replication_slot('<slot_name>');"

# Kafka topic listesi
docker exec -it kafka kafka-topics --bootstrap-server kafka:9092 --list

# Kafka mesaj (header'larıyla) — message_type header'ını görmelisin
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 --topic PRODUCT \
  --from-beginning --property print.headers=true \
  --property print.key=true --max-messages 3

# Elasticsearch index sayısı
curl -s localhost:9200/products/_count | jq

# Elasticsearch tek dokuman
curl -s localhost:9200/products/_doc/<id> | jq

# Flyway migration durumu
docker logs <service-name> 2>&1 | grep -iE 'migrat|flyway' | head

# Kafdrop UI görsel doğrulama: http://localhost:9000
```

---

## ⚠️ Tuzaklar (geçmişte takıldığımız yerler)

Detay için `CONVENTIONS.md` §16'ya bak. Özet:

- **Debezium `additional.placement`** olmadan sessizce çalışmaz (default `type` kolonu arar)
- **`@Builder` vs `@SuperBuilder`** parent + subclass uyumlu olmalı, karışım → parent field'lar kaybolur
- **`JpaRepository<Inbox, Long>` bug** — PK `String` olmalı (`BaseInbox.messageId @Id VARCHAR`)
- **Feign client URL env default** — test'te `application.clients.X.url` yoksa context load fail
- **`envsubst` whitelist** — Debezium JSON'daki `${routedByValue}` gibi placeholder'ları expand etme
- **Replication slot UUID→BIGINT geçişi** — connector silmek yetmez, slot'u da drop et
- **`@Transactional` self-invocation** — aynı sınıf metot çağrısı proxy'den geçmez, tx açılmaz
- **Hibernate lazy + `@Cacheable`** — entity cache'leme → `LazyInitializationException`, sadece `*Info` döndür

---

## Update protokolü

Claude Code bu dosyayı her iş başında **okur**, bitirdiği işi işaretler (`- [x]`) veya "Tamamlanmış" bölümüne taşır, yeni bulduğu borcu ekler. Kullanıcıya danışmadan **silme**. Format bozma (başlık hiyerarşisi, zorluk etiketi S/M/L/XL, tarih formatı).

"Aktif" bölümündeki bir item tamamen bittiğinde (test dahil, verify dahil), top-level `✅ Tamamlanmış` bölümüne taşı. Üzerine dönme.
