# ARCHITECTURE.md

Sistemin genel mimarisi, servis topolojisi, event/veri akışı ve güvenlik modeli.

> **Bitirme + staj + TÜBİTAK 2209-A projesi.** Şu an dev ortamı dışında deploy yok.
> Reponun README'sinde `images/architecture-diagram.png` ve `images/basic-order-diagram.png`
> görsel özetleri var; bu dosya yazılı detay versiyonu.

## 1. Üst seviye yapı

```
                 ┌────────────────┐
                 │    Browser     │  (React SPA + Keycloak JS)
                 │   :5173 dev    │
                 └────────┬───────┘
                          │ HTTPS
                          ▼
                 ┌────────────────┐
                 │  api-gateway   │  :9050  Spring Cloud Gateway (WebFlux)
                 │  (routing +    │  — JWT doğrulaması burada da var
                 │   JWT decode)  │  — /api/v1/public/** permitAll
                 └────────┬───────┘
                          │ (JWT forward — FeignClientInterceptor)
          ┌───────────────┼───────────────────────────────────┐
          │               │                                   │
          ▼               ▼                                   ▼
  ┌─────────────┐  ┌─────────────┐   ... ┌─────────────┐
  │ product-svc │  │  user-tenant│       │  stock-svc  │
  │    :8084    │  │  -svc :8081 │       │    :8087    │
  └──────┬──────┘  └──────┬──────┘       └──────┬──────┘
         │ JPA            │ JPA                 │ JPA
         ▼                ▼                     ▼
  ┌──────────────────────────────────────────────────┐
  │          PostgreSQL 16  (wal_level=logical)      │
  │   product_catalog_db │ user_tenant_db │ stock_db │
  │   payment_db │ keycloak_db                       │
  └────────────┬─────────────────────────────────────┘
               │ logical replication
               ▼
        ┌──────────────┐   ┌────────────┐
        │  Debezium    │──▶│   Kafka    │  topics = aggregateType
        │  2.7.3.Final │   │   7.6.0    │   "PRODUCT", "STOCK"
        └──────────────┘   └─────┬──────┘
                                 │
                                 ▼
                        ┌────────────────┐
                        │  search-svc    │  :8085
                        │ (ES consumer)  │
                        └────────┬───────┘
                                 │
                                 ▼
                        ┌────────────────┐
                        │ Elasticsearch  │  products index
                        └────────────────┘
```

Altyapıdaki diğer bileşenler:
- **Redis** (:6379) — tüm servislerde cache + idempotency; basket-service'te primary store; user-tenant-service'te authz cache
- **MinIO** (:9005 API / :9001 console) — ürün/mağaza/profil görsel depolama
- **Keycloak** (:8080) — OAuth2/OIDC; `E-Commerce` realm; `e-commerce-frontend` public client; özel SPI (user-sync, delete validation, user-service health check)
- **Kafdrop** (:9000) — Kafka UI
- **Zipkin / Prometheus / Grafana** — observability (README'de yer alıyor, kodda `management.tracing.zipkin.endpoint` set, ama ana `docker-compose.yml`'de container'ları yok; `infrastructure/devops/` altında ayrı bir compose iskeleti var)
- **Mailhog** (:8025) — lokal SMTP; mail-service planlandığında kullanılacak
- **Resilience4j** — şu an sadece `user-tenant-service`'te `payment-service` Feign çağrısı için circuit breaker + retry + time-limiter config'li (`application.yml` `resilience4j:` bloğu)

## 2. Backend modül listesi

Maven multi-module değil; her servis kendi başına bir Maven projesi ve `common-lib` + `event-contracts` yerel `SNAPSHOT` jar'ları olarak kullanılıyor.

### 2.1 Mevcut modüller

| Modül | Tür | Port | DB | Ne yapar |
|---|---|---|---|---|
| `common-lib` | library jar | — | — | Security config, `JwtAuthConverter`, `FeignClientInterceptor`, `BaseEntity/BaseOutbox/BaseInbox`, `InboxStatus` enum'u, `GlobalExceptionHandler`, `@Idempotent` AOP, `@CurrentUser` argument resolver, `TenantSecurityEvaluator`, paylaşılan DTO'lar (PageResponse vs.). Auto-config: `CommonSecurityAutoConfiguration`. |
| `event-contracts` | library jar | — | — | Sadece event payload `record`'ları. `com.ecommerce.contracts.event.product.*`, `...event.stock.*`. Producer + consumer paylaşır. **Breaking change yok, sadece additive.** |
| `api-gateway` | Spring Cloud Gateway | 9050 | — | Route + JWT filter + CORS. Reactive (WebFlux). `GatewaySecurityConfig`. |
| `user-tenant-service` (UTS) | Spring Boot MVC | 8081 | user_tenant_db | User + Tenant + Membership + Address. Authz merkezi (`InternalAuthzController` → `TenantSecurityEvaluator` diğer servislerden Feign ile buraya sorar). Keycloak SPI'dan user-sync endpoint'i. Tenant yaşam döngüsü + iyzico SubMerchant (payment-service Feign, Resilience4j korumalı). MinIO logo upload. |
| `payment-service` | Spring Boot MVC | 8082 | payment_db | iyzico entegrasyonu (ödeme + subscription + SubMerchant). Strategy pattern (SUBSCRIPTION vs PRODUCT_ORDER). Abonelik yenileme scheduler (`SubscriptionRenewalService`, `@Scheduled("0 0 3 * * ?")`). |
| `product-service` | Spring Boot MVC | 8084 | product_catalog_db | Product CRUD, Category (ağaç, slug'lı), Review. Outbox → Kafka (`PRODUCT_CREATED_EVENT`, `PRODUCT_UPDATED_EVENT`, `PRODUCT_DELETED_EVENT`). |
| `search-service` | Spring Boot MVC | 8085 | — (Elasticsearch) | `ProductEventConsumer` `PRODUCT` + `STOCK` topic'lerini dinler, Elasticsearch `products` indeksini günceller. `PublicProductSearchController` arama endpoint'i (filtre + pagination + sort). |
| `basket-service` | Spring Boot MVC | 8086 | — (Redis) | Kullanıcı sepeti. Redis primary store (`@RedisHash`, TTL 7 gün). Redisson `RLock` ile distributed lock. Product'ı Feign ile doğrular. |
| `stock-service` | Spring Boot MVC | 8087 | stock_db | Warehouse + Stock + StockMovement. Outbox → Kafka (`STOCK_RESERVED_EVENT`, `STOCK_STATUS_CHANGED_EVENT`). Pessimistic lock sipariş rezervasyonunda; optimistic `@Version` manuel güncellemede. |
| `keycloak-spi` | Keycloak plugin jar | — | — | `z-custom-spi.jar` olarak build edilir, `infrastructure/keycloak/providers/` içine kopyalanır. UserSync event listener (register/update/delete → UTS HTTP), pre-registration user-service availability check, smart delete action. |

### 2.2 Planlanmış, henüz yazılmamış modüller

README'de belirtilen ve kısmen altyapısı açılmış ama kodu yok:

| Modül | Durum | Not |
|---|---|---|
| **order-service** (SAGA Orchestrator) | Planlı | `init.sql` → `order_db`, `saga_orchestrator_db` CREATE edilmiş. `EventConstants.AGGREGATE_ORDER` var. README'de "orchestrated SAGA" olarak belirtilmiş: Order → Payment → Stock, compensating transaction'lar. `ProductPaymentStrategy.calculatePrice()` şu an mock (`return ZERO`) — order-service geldiğinde bu dolacak. |
| **mail-service** (Notification) | Planlı | `init.sql` → `notification_shipment_db`. Mailhog altyapıda hazır. Event-driven: `PaymentFailed`, `OrderShipped` vb. consume edip mail atacak. |
| **AI Engine** (FastAPI) | Planlı | README'de Python/FastAPI olarak belirtilmiş. İzole ML pipeline. Planlı özellikler: NLP tabanlı ürün yorum özetleme (`Product.aiReviewReport` field'ı bunun için), akıllı öneri, LLM virtual assistant. |

Bunlar `TODO.md`'deki "İleride yapılacaklar"da değil; **README'de vaat edilmiş** özellikler — proje vitrini.

## 3. Event pipeline (outbox → Debezium → Kafka → consumer)

Bu sistemin en kritik parçası. **Servisler Kafka'ya doğrudan publish etmez**; transactional outbox pattern kullanır.

### 3.1 Producer tarafı (product-service, stock-service)

1. İş servisi DB işlemi yapar (örn. `productRepository.save(product)`).
2. Aynı `@Transactional` içinde `outboxService.publishXEvent(...)` çağrılır. Bu metot `@Transactional(propagation = MANDATORY)` ile işaretlidir — ayrı transaction açmaz, **dış transaction'a zorunlu olarak iliştirilir**.
3. `OutboxService` payload'ı (event-contracts'taki record) JSON string'e çevirir ve `outbox` tablosuna bir satır INSERT eder:
   ```
   aggregate_type  VARCHAR — "PRODUCT" | "STOCK"        → Kafka topic adı
   aggregate_id    VARCHAR — entity id string'i          → Kafka message key
   message_type    VARCHAR — "PRODUCT_CREATED_EVENT"     → Kafka header "message_type"
   message_payload JSONB   — event payload JSON'u        → Kafka message value
   created_at      TIMESTAMP
   ```
4. İş transaction'ı commit olunca iki şey kalıcılaşır: iş verisi + outbox satırı. Atomicity garantili.

**Publish edebilen servisler:** product-service ✅, stock-service ✅.
**Publish şeması hazır, publish kodu yazılmamış:** user-tenant-service (Outbox entity + tablo var, publish eden service yok), payment-service (kendi inline Outbox entity'si var — henüz `BaseOutbox`'a migrate edilmedi, publish yok).

### 3.2 Debezium (CDC) tarafı

`infrastructure/debezium/*.json` dosyaları Debezium Connect'e kaydedilir (`register_connector.sh`). Her servis için bir connector:

- `product-service-connector` → `product_catalog_db.public.outbox`
- `stock-service-connector` → `stock_db.public.outbox`
- `user-tenant-service-connector` → `user_tenant_db.public.outbox`

### Debezium Outbox Connector Standardı

Her service connector'ı şu standartta olmalı:
- `transforms.outbox.route.by.field`: `aggregate_type`
- `transforms.outbox.table.field.event.key`: `aggregate_id`
- `transforms.outbox.table.field.event.payload`: `message_payload`
- `transforms.outbox.table.field.event.timestamp`: `created_at`
- `transforms.outbox.table.fields.additional.placement`: `"message_type:header:message_type"`
  ← DB'deki `message_type` kolonunu Kafka header'ına `message_type` ismiyle koyar.
  Consumer `@Header(value = "message_type")` ile okur. EKSİK KALIRSA EVENT AKIŞI SESSİZCE KIRILIR.
- DB outbox tablosunda: `ALTER TABLE outbox REPLICA IDENTITY FULL;` — Debezium için zorunlu.

Her connector Debezium'un **EventRouter SMT**'sini kullanır:
```json
"transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
"transforms.outbox.route.topic.replacement": "${routedByValue}",
"transforms.outbox.table.field.event.key":     "aggregate_id",
"transforms.outbox.table.field.event.payload": "message_payload",
"transforms.outbox.table.field.event.timestamp": "created_at",
"transforms.outbox.route.by.field": "aggregate_type",
"transforms.outbox.table.fields.additional.placement": "message_type:header:message_type"
```

Yani:
- Kafka **topic adı** = `outbox.aggregate_type` (`PRODUCT`, `STOCK`)
- Kafka message **key** = `outbox.aggregate_id`
- Kafka message **value** = `outbox.message_payload` (JSONB içeriği, string olarak escape'li)
- Kafka message **header** `message_type` = `outbox.message_type` ("PRODUCT_CREATED_EVENT" vs.)

> **Tuzak:** Debezium EventRouter default'ta `event_type` kolonu arar. **`table.fields.additional.placement` explicit verilmezse** kolon adı uyuşmazlığından susar (error log'u yazmadan mesaj gitmez). Bizim kolon adımız `message_type` ve header'a `message_type` olarak çıkıyor.

**REPLICA IDENTITY FULL** outbox tablosu için aktif (product-service'te `V2__outbox_CDC_config.sql` migration'ı bunu yapıyor). stock-service `V3`'te outbox yeniden yaratılıyor — `REPLICA IDENTITY FULL` eklendi mi teyit gerekli (TODO). user-tenant-service'te de henüz set edilmedi.

### 3.3 Consumer tarafı (search-service)

`ProductEventConsumer` iki topic dinler:

```java
@KafkaListener(topics = EventConstants.AGGREGATE_PRODUCT, groupId = "search-service-group")
public void consumeProductEvents(
        String messagePayload,
        @Header(value = "message_type", required = false) String headerEventType) {
    String unescapedJson = objectMapper.readValue(messagePayload, String.class);
    switch (headerEventType) {
        case EventConstants.EVENT_PRODUCT_CREATED -> handleProductCreated(unescapedJson);
        case EventConstants.EVENT_PRODUCT_UPDATED -> handleProductUpdated(unescapedJson);
        case EventConstants.EVENT_PRODUCT_DELETED -> handleProductDeleted(unescapedJson);
        case null, default -> log.warn("Bilinmeyen PRODUCT event tipi: {}", headerEventType);
    }
}
```

Önemli detay: `messagePayload` **çift serialize**. Debezium JSONB içeriğini JSON string olarak yayınladığı için önce `objectMapper.readValue(messagePayload, String.class)` ile unwrap, sonra gerçek record'a deserialize.

### 3.4 Inbox (consumer idempotency)

`common-lib/BaseInbox`:
```
message_id  VARCHAR PK  — Kafka message'in unique ID'si (idempotency key)
event_type  VARCHAR
payload     JSONB
status      VARCHAR (PENDING|PROCESSED|FAILED) — common-lib/InboxStatus enum'u
retry_count, error_message, received_at, processed_at
```

`common-lib/constant/InboxStatus.java` `PENDING`, `PROCESSED`, `FAILED` enum'u. Tüm servisler bunu import etmeli — kendi lokal `InboxStatus` enum'u yaratma.

Kullanım pattern'i (stock-service `InboxServiceImpl`):
```java
@Transactional(propagation = REQUIRES_NEW)
public boolean isMessageProcessed(String messageId, String eventType, String payload) {
    try {
        Inbox m = Inbox.builder()
                .messageId(messageId).eventType(eventType).payload(payload)
                .status(InboxStatus.PROCESSED)
                .build();
        inboxRepository.saveAndFlush(m);
        return false;  // yeni mesaj, işlenmeli
    } catch (DataIntegrityViolationException e) {
        return true;  // PK çakıştı → daha önce işlenmiş, atla
    }
}
```

Şu an sadece **şema ve helper** var; `search-service` consumer'ı henüz inbox kontrolü **yapmıyor**. Kafka at-least-once semantiği olduğu için bu açık bir boşluk — TODO.md'de.

### 3.5 Tanımlı event'ler (bugün kodda var olanlar)

`common-lib`'teki `EventConstants`:
- Aggregate: `PRODUCT`, `ORDER` (henüz kullanımda değil, order-service planlı), `STOCK`
- Event tipleri: `PRODUCT_CREATED_EVENT`, `PRODUCT_UPDATED_EVENT`, `PRODUCT_DELETED_EVENT`, `STOCK_RESERVED_EVENT`, `STOCK_FAILED_EVENT`, `STOCK_STATUS_CHANGED_EVENT`

Event payload'ları `event-contracts`'te:
- `ProductCreatedEventPayload`, `ProductUpdatedEventPayload`, `ProductDeletedEventPayload`
- `StockReservedEventPayload`, `StockStatusChangedEventPayload`

## 4. Senkron iletişim (Feign)

### 4.1 Client ↔ Adapter pattern

Her servis bağımlılık kurduğu servis için **iki sınıf** tutar:
- `client/XxxClient.java` — `@FeignClient` arayüzü (raw HTTP)
- `client/adapter/XxxClientAdapter.java` — `@Component` sarmalayıcı, FeignException'ı yakalar ve domain exception'larına çevirir (`BusinessException`, `ExternalServiceException`, `ResourceNotFoundException`)

Service katmanı **Adapter**'ı inject eder, Client'a direkt erişmez.

Örnek (stock-service → product-service):
```java
// client/ProductClient.java
@FeignClient(name = "product-service", url = "${application.clients.product.url}")
public interface ProductClient {
    @GetMapping("/api/v1/products/tenants/{tenantId}/{productId}")
    ProductResponse getTenantProduct(@PathVariable Long tenantId, @PathVariable Long productId);
}

// client/adapter/ProductClientAdapter.java
@Component @RequiredArgsConstructor
public class ProductClientAdapter {
    private final ProductClient productClient;
    public ProductResponse validateAndGetProduct(Long productId, Long tenantId) {
        try { return productClient.getTenantProduct(tenantId, productId); }
        catch (FeignException.NotFound | FeignException.BadRequest e) {
            throw new BusinessException("Bu ürün size ait değil veya bulunamadı!", "PRODUCT_NOT_OWNED");
        } catch (Exception e) {
            throw new ExternalServiceException("Ürün doğrulaması yapılamadı", "PRODUCT_SERVICE_DOWN");
        }
    }
}
```

### 4.2 JWT forwarding

`common-lib`'teki `FeignClientInterceptor` her Feign request'ine SecurityContext'ten JWT'yi `Authorization: Bearer ...` olarak ekler. `GlobalSecurityConfig`'te:
```java
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
```
Alt thread'lere (async, scheduled) context geçsin diye.

### 4.3 Resilience4j (şu an sadece UTS → payment-service)

`user-tenant-service/application.yml`:
```yaml
resilience4j:
  circuit breaker:
    instances:
      payment-service:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 5s
        ...
  retry:
    instances:
      payment-service: { maxAttempts: 3, waitDuration: 2s }
  time limiter:
    instances:
      payment-service: { timeoutDuration: 10s }
```
`PaymentServiceFallback` sınıfı yorum satırı içinde — fallback aktif değil. Diğer servislerin Feign çağrılarında resilience4j yok. README "System Resilience" olarak bu pattern'i vurguluyor ama yayılım eksik.

### 4.4 Feign endpoint'leri (bugün kodda var)

| Kaynak → Hedef | Ne için |
|---|---|
| herhangi servis → user-tenant-service `/api/v1/internal/authz/users/{userId}/tenants/{tenantId}/role` | `TenantSecurityEvaluator` cache miss'te rol sorar |
| user-tenant-service → payment-service `/api/v1/payments/process` | Tenant oluştururken abonelik ödemesi |
| user-tenant-service → payment-service `/api/v1/subscriptions/tenants/{id}` | Abonelik detayı |
| user-tenant-service → payment-service `/api/v1/payments/history/...` | Ödeme geçmişi |
| user-tenant-service → payment-service `/api/v1/submerchant/create\|update` | iyzico SubMerchant CRUD |
| stock-service → product-service `/api/v1/products/tenants/{tid}/{pid}` | Manuel stok eklerken ürün doğrulama |
| basket-service → product-service `/api/v1/public/products/{id}` | Sepete eklerken ürün doğrulama + fiyat/isim/resim |

### 4.5 URL config

Her servisin `application.yml`'inde:
```yaml
application:
  clients:
    user-tenant: { url: ${USER_TENANT_SERVICE_URL} }
    product:     { url: ${PRODUCT_SERVICE_URL} }
```
`.env` üzerinden verilir.

> **Tuzak:** Test ortamında Feign client bean'i init olurken `application.clients.X.url` env değişkeni yoksa context load'da patlar. WireMock'a bağlanan test'lerde `@TestPropertySource` ile `localhost:${wiremock.server.port}` verilir; diğer durumlarda `.env.test` hazır olmalı.

## 5. Güvenlik modeli

### 5.1 Auth flow

1. Frontend (`react-oidc-context`) Keycloak'a redirect eder → kullanıcı login olur.
2. Keycloak **access token (JWT)** döner. Frontend her isteğe `Authorization: Bearer ...` ekler.
3. API Gateway ve her backend servisi JWT'yi `issuer-uri` üzerinden Keycloak'ın public key'i ile doğrular (`oauth2ResourceServer.jwt`).
4. `JwtAuthConverter` (common-lib) JWT claim'lerinden rolleri çıkarır — Keycloak `resource_access.<clientId>.roles` altındaki rolleri `ROLE_xxx` olarak `GrantedAuthority`'e ekler.
5. Principal (`sub` claim → UUID) → `AuthUser` record'u `CurrentUserArgumentResolver` ile controller metoduna `@CurrentUser AuthUser user` olarak inject edilir.

### 5.2 Tenant RBAC ve Shared Schema multi-tenancy

README'nin vurgusu: "**Shared Schema** approach on PostgreSQL to ensure strict data isolation across different merchant tenants." Yani her tenant için ayrı DB veya ayrı schema YOK. Aynı tablolar, `tenant_id BIGINT NOT NULL` kolonu ile tenant sınırı çizilir. `products.tenant_id`, `stocks.tenant_id`, `warehouses.tenant_id` vb. hepsinde.

Bu pattern'in sonuçları:
- Her query'de `tenant_id` WHERE filter'ı zorunlu (repository method isimleri `findByIdAndTenantId` gibi).
- Controller'larda `@PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")` ile tenant sahipliği kontrolü şart.
- **Cross-tenant data leak** riski tamamen kodun disiplinine bağlı — şema seviyesi koruma yok.

`common-lib`'teki `TenantSecurityEvaluator` (bean adı: `"tenantSecurity"`) iki metot sunar:

```java
@PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")  // belirli rol gerekli
@PreAuthorize("@tenantSecurity.isMember(#tenantId)")          // sadece üye olmak yeterli
```

Çalışma mantığı:
1. Önce Redis hash'e bakar: `authz:user:{keycloakId}` → field `tenant_{tenantId}` → rol ("OWNER"/"ADMIN"/"STAFF"/"ACCOUNTANT" veya "NONE").
2. Cache miss → UTS'nin `/api/v1/internal/authz/users/{userId}/tenants/{tenantId}/role` endpoint'ine Feign ile sorar. Dönen rolü Redis'e yazar (`AuthzCacheService`, TTL 2 saat).
3. Rol `OWNER` ise her `hasRole` true döner. Aksi halde tam eşleşme beklenir.

Rol/üyelik değiştiğinde (member add/update/remove) UTS `authzCacheService.evictUserCache(keycloakId)` çağırarak hash'i siler. **Cache invalidation kritik** — dokunulunca unutma.

### 5.3 Public endpoint'ler

`common-lib/GlobalSecurityConfig`:
```
/actuator/health, /actuator/prometheus  → permitAll
/v3/api-docs/**, /swagger-ui/**          → permitAll
/api/v1/public/**                        → permitAll
/api/v1/categories/**                    → permitAll
diğer her şey → authenticated
```
`/api/v1/internal/**` şu an **ayrı koruma altında değil** — authenticated olduğu için Feign interceptor'dan gelen JWT yetiyor ama prod'a çıkarken network-level kısıt (service mesh, internal ip) gerekir.

### 5.4 Keycloak SPI

`backend/keycloak-spi/` üç yetenek ekler:

1. **UserSyncEventListenerProvider** — Keycloak'ta `REGISTER`, `UPDATE_PROFILE` (user), `DELETE` (admin) event'lerini yakalar → UTS'nin `/api/v1/users/` endpoint'ine HTTP POST/PUT/DELETE atar. Service Account token ile kimliklenir. Hata olursa Keycloak transaction'ını rollback yapar (register için user'ı siler).
2. **UserServiceAvailabilityValidator** + **UserServiceValidationAction** — kayıt ekranında "User Service ayakta mı?" kontrolü, yoksa kayıt olmayı engeller.
3. **SmartDeleteAccountAction** — Required Action; kullanıcının kendisini silmesi senaryosu.

Derleme: `./infrastructure/scripts/build-spi.sh` → `infrastructure/keycloak/providers/z-custom-spi.jar`. Keycloak başlarken `providers/` klasörünü tarar.

## 6. Redis kullanım haritası

| Amaç | Key pattern | Tipi | Kim kullanır | Not |
|---|---|---|---|---|
| Product entity cache | `tenant-product::{tenantId}:{productId}` | `ProductInfo` record | product-service | `@Cacheable` |
| Public product cache | `public-product::{productId}` | `PublicProductInfo` | product-service | `@Cacheable`, invalidate on update/delete |
| Kök kategoriler | `categories-root::SimpleKey []` | `List<CategoryInfo>` | product-service | `@Cacheable` |
| Kategori slug | `category-by-slug::{slug}` | `CategoryInfo` | product-service | `@Cacheable` |
| Stok bilgisi | `stock::{tenantId}:{warehouseId}:{productId}` | `StockInfo` | stock-service | `@Cacheable` / `@CacheEvict` |
| Tenant storefront | `public-tenant-storefront::{tenantId}` | `TenantStorefrontInfo` | UTS | `@Cacheable` |
| Sepet (primary store) | `Basket:{userId}` | `Basket` entity (`@RedisHash`) | basket-service | TTL 604800s (7 gün) |
| Sepet lock | `lock:basket:{userId}` | Redisson RLock | basket-service | 10s wait / 5s lease |
| Authz rol | `authz:user:{keycloakId}` (Hash) → `tenant_{tenantId}` | String | UTS + TenantSecurityEvaluator | TTL 2h |
| Idempotency | `idempotency:<prefix>:{headerValue}` | String "PROCESSING" | `@Idempotent` AOP (common-lib) | Default TTL 86400s (24h) |

## 7. MinIO bucket yapısı

`user-tenant-service/ImageService`:
- Bucket: `${USER_MINIO_BUCKET}` (env'den)
- Object key'i: `<folderName>/<uuid>_<originalName>`
  - `profiles/...` — user profile photos
  - `tenants/...` — tenant logoları
- URL döner: `{MINIO_URL}/{bucket}/{object}`

Diğer servisler şu an MinIO'ya yazmıyor. **Ürün görseli upload endpoint'i**: frontend bunu product-service'ten bekliyor (`POST /api/v1/products/images/upload`, folderName `"products"`), **backend tarafı henüz yazılmadı**. Bu TODO.md'de "Frontend ↔ Backend açık talepler" bölümünde.

Prod aşamasına geçildiğinde MinIO'nun nginx reverse proxy arkasına alınması gerekir (signed URL / access control için).

## 8. Observability

Her servisin `application.yml`'inde:
- Spring Boot Actuator: `/actuator/health`, `/actuator/prometheus`, `/actuator/metrics`
- Micrometer Tracing + Zipkin endpoint: `http://localhost:9411/api/v2/spans`
- `management.tracing.sampling.probability: 1.0` (dev'de %100 sample)

`infrastructure/devops/`'da `prometheus.yml` ve `promtail-config.yml` var (ayrı compose stack). Ana `docker-compose.yml`'e Zipkin / Prometheus / Grafana container'ları henüz eklenmedi — kod traces/metrics üretiyor ama dev'de toplayacak endpoint yok. TODO.

## 9. Frontend özet

- **Vite 7 + React 19 + TS 5.9**
- **Zustand** (`useAuthStore`, `useCartStore`, `useMerchantStore`, `useCategoryStore`, `useToastStore`) — client state
- **@tanstack/react-query** — server state (`useProductQueries`, `useBasketQueries`, `useUserQueries`, `useTenantQueries`)
- **react-oidc-context** — Keycloak OIDC akışı
- **MUI 7** — UI components
- **Axios** — HTTP, tek instance (`lib/axios.ts`), `Authorization` header otomatik
- **Routing** — React Router 7, code split (`lazy()`)
- **Cart davranışı:** auth'suz kullanıcı için `useCartStore` (in-memory/local), login olunca `localCartItems` basket-service'e push'lanıp temizlenir. Auth'lu kullanıcı için doğrudan backend cart (`basket-service`).
- **Keycloakify** — Keycloak login temasını React ile custom'laştırma
- **Görsel yükleme:** `ImageUploadField` componenti (`SingleImageUpload` + `MultiImageUpload`). Dosya seçilince önce `URL.createObjectURL()` ile anlık önizleme, arka planda `productService.uploadProductImage(file)` multipart upload çağrılır, MinIO URL döner ve state'e yazılır. Base64 yok (eski sürümdeki dataUrl pattern'i kaldırıldı). Form submit upload tamamlanana kadar bloke edilir (`isProductFormValid` içinde kontrol).
- **Edit formu** (`MerchantProductForm`) artık list response yerine detail endpoint (`useGetTenantProductById`) ile doluyor — `description`/`attributes` gibi full alanlar için gerekli.

## 10. Bilinen mimari pürüzler ve migration durumu

Sistem açık bir **aşamalı migration** sürecinde. `TODO.md`'nin top section'ına da yansıdı; burada mimari çerçevede ne olup bittiğine bakıyoruz.

### 10.1 Tamamlanmış (Stage 1 & 2)

- ✅ **Stage 1:** product-service event akışı ÇALIŞIYOR. Outbox → Debezium → Kafka → consumer zinciri end-to-end sağlam. `common-lib/BaseOutbox` genişletilmiş, product-service entity'si bunu extend ediyor. Migration V2 `REPLICA IDENTITY FULL` ekledi.
- ✅ **Stage 2:** search-service consumer `event-contracts`'taki record'lara geçti. Eski inline consumer DTO'ları silindi. `@Header("message_type")` kullanımı Debezium config'iyle tutarlı.
- ✅ **common-lib**'te `BaseInbox` zenginleştirildi (`status`, `retry_count`, `error_message`, `received_at`, `processed_at`), `InboxStatus` enum'u `com.ecommerce.common.constant` altına taşındı.

### 10.2 Aktif (Stage 3) — bu sprint

- **stock-service migration**: Outbox/Inbox entity'lerinin `BaseOutbox`/`BaseInbox` extend'ini tamamla. Kalan sorunlar: `Outbox.java` `@GeneratedValue(strategy = GenerationType.UUID)` hatalı (Long field + BIGINT IDENTITY kolonu), `InboxService` interface boş (metot Impl'de), `StockInfo` yanlış pakette (`entity/` yerine `query/` olmalı + `Serializable`). Migration `V3__migrate_outbox_id_and_enrich_inbox.sql` DB tarafını kısmen çözdü, Java tarafı tamamlanacak.
- **Debezium stock-service-connector** push'u: Slot drop + connector re-register. Script: `infrastructure/scripts/register_connector.sh`.
- Detay: `TODO.md` "Stage 3" bölümü.

### 10.3 Sıradaki (Stage 4)

- **payment-service**: kendi inline `Outbox`, `Inbox`, `InboxStatus` enum'u var. `BaseOutbox`/`BaseInbox`/`common-lib.InboxStatus`'a migrate edilecek. `event-contracts` dependency eklenecek. Publish senaryoları kararlaştırılacak (ödeme başarılı/başarısız event'leri?).
- **user-tenant-service**: Outbox entity + tablo var, `BaseOutbox` extend etmiyor. Publish kodu yok. Önce entity migrate, sonra hangi event'lerin yayılacağına karar.
- Detay: `TODO.md` "Stage 4" bölümü.

### 10.4 Uzun vadeli planlı (README'de vaat edilen)

- **order-service** (SAGA Orchestrator): en büyük eksik. README'nin ön planında, sipariş akışının merkezi. `ProductPaymentStrategy.calculatePrice()` mock — bu servis geldiğinde dolacak.
- **mail-service**: event-driven notification consumer. Mailhog hazır.
- **AI Engine (FastAPI)**: ML pipeline, review summary, recommendation, LLM assistant.

### 10.5 Diğer teknik borç (migration zinciri dışı)

- DLQ (Dead Letter Queue) yok Kafka consumer'larında
- Consumer inbox idempotency kullanılmıyor (şema hazır, search-service kullanmıyor)
- Outbox cleanup scheduler payment-service'te yok (product, stock'ta var)
- api-gateway prod profili yarım (sadece user-tenant-service route'u)
- `.env.example` eksik (kodda 20+ env var kullanılıyor, örnekte 7)
- Resilience4j sadece UTS → payment-service için; diğer Feign çağrılarında yok
- Observability stack compose'a eklenmedi (Zipkin, Prometheus, Grafana)

## 11. Yeni bir servis eklerken

Minimum checklist:

1. `backend/<yeni-service>/` — `pom.xml` (parent: `spring-boot-starter-parent:3.5.9`), `mvnw`, `src/main/java/com/ecommerce/<yenis>/`
2. `common-lib` ve (event yayıyorsa) `event-contracts` dependency
3. `application.yml` + `application-dev.yml` — Actuator, JWT, cache type (Redis kullanıyorsa), tracing sampling
4. `<n>ServiceApplication.java` — `@SpringBootApplication`, component scan/entity scan ile `com.ecommerce.common`'u dahil et
5. `common/config/CoreConfig.java` — `@ComponentScan("com.ecommerce.common")`, `@EntityScan(...)` her iki paket için, `@EnableCaching` (kullanıyorsa)
6. `common/constants/ApiPaths.java` — URL sabitleri
7. DB gerekiyorsa: `init/init.sql`'e `CREATE DATABASE <yeni>_db;` satırı ekle
8. Flyway migration V1
9. Event publish ediyorsa: outbox tablosu + `Outbox extends BaseOutbox` entity + `OutboxService`; Debezium connector JSON'u `infrastructure/debezium/<yeni>-connector.json`; tabloya `ALTER TABLE outbox REPLICA IDENTITY FULL`
10. Event dinliyorsa: inbox tablosu + `Inbox extends BaseInbox` + `InboxService` + `@KafkaListener`
11. Gateway route'u: `api-gateway/src/main/resources/application-dev.yml`
12. Docker-compose'a eklemek (prod build istenirse)
13. Port'u `CLAUDE.md`'deki haritaya ekle

Detaylı kod örnekleri için `CONVENTIONS.md`.
