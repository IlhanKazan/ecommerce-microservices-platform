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
- **Stage 9.0: user-tenant-service container** ✅ — Dockerfile (BuildKit cache, non-root), prod.yml (HikariCP, JWT dual-mode, Flyway retry), compose wiring. "Started" logu doğrulandı. — Kafka consumer, Thymeleaf şablonları, inbox idempotency, docker-compose. TENANT_ACTIVATED + TENANT_PAYMENT_FAILED çalışıyor. Eksik event bağlantıları (VERIFIED, PAYMENT_SUCCESS, ORDER_*) → `SERVICE-WORK.md` "Mail Service" bölümüne taşındı.

---

## ⏭️ Aktif

### Stage 9: Tüm servisleri container'a çekme (prod profili geçişi)

#### Kritik bilgi — okumadan başlama

**Dockerfile pattern** (UTS'den öğrenildi, tüm servisler aynı):
```dockerfile
# syntax=docker/dockerfile:1
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY common-lib/pom.xml ./common-lib/pom.xml
COPY common-lib/src ./common-lib/src
RUN --mount=type=cache,target=/root/.m2 cd common-lib && mvn install -DskipTests -q
COPY event-contracts/pom.xml ./event-contracts/pom.xml
COPY event-contracts/src ./event-contracts/src
RUN --mount=type=cache,target=/root/.m2 cd event-contracts && mvn install -DskipTests -q
COPY <servis-adi>/pom.xml ./<servis-adi>/pom.xml
COPY <servis-adi>/src ./<servis-adi>/src
RUN --mount=type=cache,target=/root/.m2 cd <servis-adi> && mvn clean package -DskipTests -q
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/<servis-adi>/target/*.jar app.jar
USER spring
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java","-XX:+UseContainerSupport","-XX:MaxRAMPercentage=75.0","-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]
```

**JWT dual-mode pattern** (tüm prod.yml'lere — detay: `ARCHITECTURE.md §5.1`):
```yaml
security:
  oauth2:
    resourceserver:
      jwt:
        jwk-set-uri: http://keycloak:8080/realms/${REALM_NAME}/protocol/openid-connect/certs
        issuer-uri: http://localhost:8080/realms/${REALM_NAME}
```
> Gateway WebFlux kullanıyor — aynı key'ler ama `spring.security.oauth2.resourceserver.jwt.*` altında.

**prod.yml Flyway retry** (DB hazır olmadan başlarsa):
```yaml
spring:
  flyway:
    connect-retries: 10
    connect-retries-interval: 3
```

**HikariCP minimum** (tüm DB'li servislere):
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 2
      maximum-pool-size: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**application-prod.yml yazma metodolojisi** — her servis için:
1. `application.yml` + `application-dev.yml`'i oku (gerçek config buradadır)
2. Host'ları container DNS'e çevir:
   - `localhost:5432` → `postgres:5432`
   - `localhost:29092` → `kafka:9092`
   - `localhost:6379` → Redis için `host: redis`
   - `localhost:8080` (jwk-set-uri'de) → `keycloak:8080` (sadece JWK fetch için)
   - `issuer-uri` **localhost kalır** — token'daki `iss` claim localhost URL taşıyor, değiştirme
   - Feign URL'ler: `${USER_TENANT_SERVICE_URL}` → `http://user-tenant-service:8081` vb.
3. Boşluklu YAML key'leri düzelt: `resource server` → `resourceserver`, `time limiter` → `time-limiter`
4. Ekle: HikariCP, Flyway retry, JWT dual-mode, `devtools.restart.enabled: false`, log seviyeleri, zipkin endpoint
5. Sil: `spring.config.import: optional:file:.env[.properties]` (prod'da env var'lar compose'dan gelir)

**⚠️ init.sql'e `stock_db` eksik** — docker-compose'da postgres bloğunun volumes mount'u `init.sql`'i çalıştırıyor ama `stock_db` yok. `infrastructure/postgres/init.sql`'e `CREATE DATABASE stock_db;` ekle. Postgres volume'ü varsa `docker compose down -v postgres` ile sil, yeniden başlat.

---

#### 9.0 user-tenant-service ✅
- [x] Dockerfile (BuildKit cache mount, non-root user)
- [x] application-prod.yml (HikariCP, Kafka, Redis, MinIO, JWT dual-mode, Flyway retry, Zipkin)
- [x] docker-compose: SPRING_PROFILES_ACTIVE=prod, tüm env var'lar map edildi
- [x] Container ayağa kalkıyor, "Started" logu görüldü

---

#### 9.1 payment-service ✅
- [x] Dockerfile yaz
- [x] application-prod.yml yaz
- [x] docker-compose servis bloğu ekle
- [x] Build + `docker compose up -d payment-service` + log kontrol — "Started" logu görüldü ✅

---

#### 9.2 product-service ✅
- [x] Dockerfile yaz
- [x] application-prod.yml yaz
- [x] docker-compose servis bloğu ekle
- [x] Build + test — "Started" logu görüldü, ürün ekleme + görsel upload çalışıyor ✅
- **Not:** Anasayfada ürün görünmüyor — search-service henüz container'da değil, Kafka event'lerini consume edemiyor. 9.4 tamamlanınca düzelecek.

---

#### 9.3 stock-service ✅
- [x] `infrastructure/postgres/init.sql`'e `CREATE DATABASE stock_db;` eklendi (zaten vardı, dokümantasyon eksikti)
- [x] Dockerfile yaz
- [x] application-prod.yml yaz
- [x] docker-compose servis bloğu ekle
- [x] Build + test — "Started" logu görüldü ✅

---

#### 9.4 search-service ✅
- [x] Dockerfile yaz
- [x] application-prod.yml yaz
- [x] docker-compose servis bloğu ekle
- [x] Build + test — "Started" logu görüldü ✅

---

#### 9.5 basket-service ✅
- [x] Dockerfile yaz
- [x] application-prod.yml yaz
- [x] docker-compose servis bloğu ekle
- [x] Build + test — "Started" logu görüldü ✅

---

#### 9.6 api-gateway ✅
- [x] Dockerfile yaz (common-lib yok, direkt build)
- [x] application-prod.yml yaz — JWT dual-mode, tüm route'lar container DNS, CORS korundu
- [x] docker-compose servis bloğu ekle
- [x] Build + test — "Started" logu görüldü ✅

---

#### 9.7 Temizlik
- [x] `connector-init` düzeltildi — `docker-init-connectors.sh` yazıldı, tüm 4 connector mount edildi, `service_healthy` ile bekleme, `restart: "no"` eklendi
- [x] `.env.example`'ı tüm değişkenlerle yaz — tüm servisler, SMTP açıklaması, Keycloak SPI dahil
- [ ] `application.yml`'lerdeki boşluklu YAML key'leri düzelt: `resource server` → `resource-server` (payment, product, search, basket dev yml'lerinde var)
- [ ] Her servis için commit: `devops(<servis>): prod profile + Dockerfile + compose wiring`

---

### Stage 11: Observability — tek compose hedefi

**Amaç:** `docker compose up -d` tek komutla tüm sistem + monitoring ayağa kalksın.

**Şu anki durum:** `infrastructure/devops/docker-compose.yml` ayrı dosyada — Prometheus, Grafana, Zipkin, Loki, Promtail, cAdvisor burada. Servisler `e-commerce-network`'e bağlı değil, scrape target'ları eski IP'ler (`172.23.0.1:8081` vb.).

**Yapılacaklar:**
- [ ] `infrastructure/devops/docker-compose.yml`'deki tüm servisleri ana `docker-compose.yml`'e taşı — volume path'leri `./infrastructure/devops/...` olarak güncelle
- [ ] Tüm observability servislerine `e-commerce-network` ekle
- [ ] `infrastructure/devops/prometheus.yml` scrape target'larını güncelle — `172.23.0.1:PORT` → `container-name:PORT` (7 servis: UTS, payment, product, stock, search, basket, gateway — hepsi `/actuator/prometheus` açık)
- [ ] Zipkin: servisler zaten `http://zipkin:9411/api/v2/spans` yazıyor — sadece `zipkin` container'ı aynı network'te olunca çalışır
- [ ] Loki + Promtail: `promtail-config.yml` Docker container log path'lerini doğru göstermeli (`/var/lib/docker/containers`)
- [ ] Grafana data source otomatik provision: Prometheus + Loki için `provisioning/datasources.yml`
- [ ] Grafana dashboard'ları: JVM (Spring Boot), Kafka consumer lag, Redis, cAdvisor container metrikler
- [ ] `infrastructure/devops/prometheus.yml`'den `cadvisor` scrape'i zaten var — cAdvisor da aynı network'e taşınınca çalışır
- **L**

---

### Stage 10: Order Service — satın alma akışı

Platform'un kalbindeki eksik parça. `order_db` ve `saga_orchestrator_db` init.sql'de zaten var. Şu an backend kodu sıfır.

#### Genel mimari

```
[Frontend Checkout]
     │  POST /api/v1/orders
     ▼
[order-service — port 8088]
     │  SAGA Orchestrator
     ├──► [stock-service Feign] reserve()
     │        ↓ başarısız → BusinessException → OrderCancelled
     ├──► [payment-service Feign] processPayment()
     │        ↓ başarısız → compensation: stock.rollback() → OrderCancelled
     └──► Order CONFIRMED
          │  outbox → Debezium → ORDER topic
          ▼
     [mail-service] ORDER_CONFIRMED_EVENT → sipariş onay maili
     [stock-service] stok commit (rezerveden gerçek düşmeye)
```

**SAGA tipi:** Senkron orkestrasyon (Feign) + outbox event. Faz 1'de async SAGA değil — Feign zinciri + compensation yeterli. Async SAGA (event-driven) ikinci iterasyonda.

#### 10.0 event-contracts genişletme
- [ ] `OrderCreatedEventPayload` record — orderId, userId, tenantId, items (snapshot), totalAmount
- [ ] `OrderConfirmedEventPayload` — orderId, userId, tenantId
- [ ] `OrderCancelledEventPayload` — orderId, userId, reason
- [ ] `OrderShippedEventPayload` — orderId, trackingNumber
- [ ] `EventConstants`'a `AGGREGATE_ORDER`, `ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_CANCELLED`, `ORDER_SHIPPED` sabitleri
- Kural: additive only, breaking change yok

#### 10.1 order-service iskelet + entity
- [ ] Spring Boot 3.5.9, Java 21 proje oluştur — `backend/order-service/`
- [ ] `pom.xml`: common-lib + event-contracts dependency, MapStruct, Lombok, Feign, Flyway, PostgreSQL
- [ ] `OrderStatus` enum: `DRAFT → SUBMITTED → CONFIRMED → SHIPPED → DELIVERED → CANCELLED`
- [ ] `Order` entity (BaseEntity extend): userId (UUID), tenantId, status, totalAmount, currency, shippingAddressJson (TEXT), paymentTransactionId
- [ ] `OrderItem` entity: orderId (FK), productId, sku, productName (**snapshot**), productImageUrl (**snapshot**), unitPrice (**snapshot**), quantity
- [ ] `SagaState` entity: sagaId (UUID PK), orderId (FK), currentStep (enum), status, compensationData (JSON), errorMessage
- [ ] Flyway V1: orders, order_items, saga_states tablolar + index'ler
- [ ] `OrderRepository`, `OrderItemRepository`, `SagaStateRepository`
- [ ] REPLICA IDENTITY FULL outbox için (V1'de)
- **M**

#### 10.2 Feign client'lar
- [ ] `StockServiceClient` — `reserve(tenantId, productId, amount)`, `rollback(tenantId, productId, amount)`, `commit(tenantId, productId, amount)`, `getStock(tenantId, productId, warehouseId)`
- [ ] `PaymentServiceClient` — `processOrderPayment(OrderPaymentRequest)` → `PaymentResult`
- [ ] `BasketServiceClient` — `getMyBasket()` → basket items (checkout için)
- [ ] `ProductServiceClient` — `getProductSnapshot(productId)` → fiyat + isim + stok snapshot
- [ ] Her client için `FeignConfig` (FeignClientInterceptor'dan geliyor, JWT forward)
- [ ] Fallback stub'ları (`@FallbackFactory` pattern) — circuit breaker hazır olsun
- **M**

#### 10.3 SAGA orchestrator servisi
- [ ] `OrderSagaService.execute(Order)` — tek transaction değil, her adım ayrı transaction
  - Adım 1: `StockServiceClient.reserve()` → başarısız → `ORDER_CANCELLED` at, RuntimeException throw etme
  - Adım 2: `PaymentServiceClient.processOrderPayment()` → başarısız → `StockServiceClient.rollback()` + `ORDER_CANCELLED`
  - Adım 3: Order `CONFIRMED`, outbox'a `ORDER_CONFIRMED_EVENT` at
- [ ] `SagaState` her adımda güncellenir (idempotency için sagaId üzerinden tekrar çalıştırılabilir)
- [ ] Compensation işlemlerinde ayrıca `SagaState.status = COMPENSATING` set et
- [ ] `@Transactional` boundary: her SAGA adımı kendi transaction'ında — tüm zincir TEK transaction değil
- **L**

#### 10.4 Order API endpoint'leri
- [ ] `POST /api/v1/orders` — sepetten sipariş oluştur
  - `@CurrentUser` ile userId, `BasketServiceClient.getMyBasket()` ile items
  - Her item için `ProductServiceClient.getProductSnapshot()` → fiyat snapshot
  - `OrderCreateContext` service'e geçer (DTO değil)
  - Service: Order + OrderItem kaydet, SAGA başlat
  - Response: `OrderSummaryResponse` (id, status, totalAmount, itemCount)
  - Idempotency-Key zorunlu
- [ ] `GET /api/v1/orders/me` — kullanıcının sipariş geçmişi (sayfalandırılmış)
- [ ] `GET /api/v1/orders/me/{orderId}` — sipariş detayı + item'lar
- [ ] `POST /api/v1/orders/me/{orderId}/cancel` — iptal (sadece SUBMITTED veya CONFIRMED, SHIPPED değil)
  - Cancellation logic: stok rollback + ödeme refund (payment-service Feign)
- [ ] `GET /api/v1/orders/tenants/{tenantId}` — mağaza sahibi için gelen siparişler (OWNER)
- [ ] `PUT /api/v1/orders/tenants/{tenantId}/{orderId}/status` — mağaza sahibi durum güncelle (SHIPPED, DELIVERED)
  - SHIPPED → `ORDER_SHIPPED_EVENT` outbox'a at (mail-service kargo takip maili atar)
- **L**

#### 10.5 Outbox + Debezium
- [ ] `Outbox` entity, `OutboxService`, `OutboxRepository` — mevcut pattern (BaseOutbox extend)
- [ ] Flyway V1'e outbox tablosu + REPLICA IDENTITY FULL ekle
- [ ] `order-service-connector.json` — Debezium connector (ORDER topic)
- [ ] `register_connector.sh`'a ekle
- [ ] Runtime verify: ORDER topic'te ORDER_CONFIRMED_EVENT mesajı gözüküyor mu
- **M**

#### 10.6 Frontend checkout akışı
- [ ] `CartPage` → "Siparişi Tamamla" butonu aktif hale getir
- [ ] Checkout adımları: Sepet özeti → Teslimat adresi seç → Ödeme onay → Sipariş tamamlandı
- [ ] `useCreateOrder` mutation (idempotency key ile)
- [ ] `useGetMyOrders`, `useGetOrderDetail` query hook'ları
- [ ] Sipariş geçmişi sayfası (`/orders`) — `MyOrdersPage`
- [ ] Sipariş detay sayfası (`/orders/{orderId}`)
- [ ] "Siparişi İptal Et" butonu (sadece iptal edilebilir statüslerde)
- [ ] `types/order.ts` — `OrderSummaryResponse`, `OrderDetailResponse`, `OrderItemResponse`
- [ ] `config/apiEndpoints.ts`'e ORDER endpoint'leri
- **L**

> **Bağımlılıklar:** stock-service'te `POST reserve`, `POST rollback`, `POST commit` endpoint'leri yazılmalı (şu an sadece manual-add var). payment-service'te `processOrderPayment` endpoint'i.

---

## 🧩 Frontend ↔ Backend arası açık talepler

### FB-1: Ürün görseli upload endpoint'i ✅
### FB-2: Tenant product detail endpoint'i ✅
### FB-4: Frontend ürün ekleme/düzenleme UI ✅

### FB-3: MinIO prod reverse proxy (not, prod roadmap)
Şu an dev'de doğrudan erişim. Prod'da signed URL + access control.

### FB-5: Tenant depo & stok detay görünümü

Mağaza sahibi depo sayfasında hangi ürünün ne kadar stoku olduğunu göremiyoruz.

**Backend (stock-service):**
- [ ] `GET /stocks/tenant/{tenantId}/summary` — tenant'ın tüm ürünleri + toplam stok (warehouse bazında breakdown ile)
  - Response: `[{ productId, sku, productName, totalAvailable, totalReserved, warehouseBreakdown: [{warehouseId, warehouseName, available, reserved}] }]`
  - Feign ile product-service'den ürün adını çek VEYA stock-service'te productName snapshotı tut (ikincisi daha sağlam — sipariş gibi snapshot pattern)
- [ ] `GET /stocks/tenant/{tenantId}/warehouses/{warehouseId}/stocks` — tek depo için stok listesi

**Frontend (MerchantWarehousePage):**
- [ ] Depo listesinin altına her depo için "Stok Detayı" toggle veya expand açılır tablo
- [ ] Stok durumu: yeşil (yeterli), sarı (eşik altı), kırmızı (sıfır)
- [ ] Ürün bazında toplam stok özeti tablosu (tüm depolar, tüm ürünler)
- [ ] Stok girişi butonunu ilgili ürüne pre-fill olarak aç (tablo satırından)
- **M**

### FB-6: Arama çubuğu autocomplete + ürün görseli

Header arama çubuğu şu an sadece Enter'da product list sayfasına yönlendiriyor. Ürün görseli yok, autocomplete yok.

**Backend (search-service):**
- [ ] `GET /public/search/autocomplete?q={term}&size=5` endpoint — hızlı öneri
  - ES `multi_match` query (name, brand, tags), sadece `name + mainImageUrl + price + id` döner
  - Debounce için hafif endpoint (ağır arama değil)

**Frontend:**
- [ ] `Header.tsx`'te MUI `Autocomplete` component — debounced (300ms) query
- [ ] Her seçenek: ürün görseli (32px avatar) + ürün adı + fiyat
- [ ] Seçilince `/products/{id}` sayfasına git
- [ ] Enter'da mevcut davranış korunur (keyword ile product list)
- [ ] Görsel yoksa placeholder avatar (baş harf)
- **M**

### FB-7: Sipariş akışı frontend (Stage 10.6 ile aynı — oradan takip et)

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
