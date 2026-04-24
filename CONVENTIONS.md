# CONVENTIONS.md

Kod tabanındaki tutarlı pattern'ler. Yeni kod yazarken bu kurallara uy. Bir şey belirsizse, aynı servisteki benzer bir örneği taban al.

> **Önemli:** Pattern'leri körlemesine uygulama — paket yapısı servisten servise hafif değişir. Yeni bir dosya eklemeden önce **o servisteki benzer bir dosyayı aç ve oku**. Varsayıma dayalı kod yazma; "muhtemelen böyledir" → "hayır değilmiş" sonucu vakit kaybı. Kullanıcı hallucination'a sıfır tolerans.

> **Not:** Bazı kurallar (👤 ile işaretli) kullanıcı tarafından açıkça belirtildi. Diğerleri koddan çıkarılan gözlemlerdir; tutarlı şekilde uygulanıyorlar ama istisnalar var — §15 ve §16'da listelendi.

## 1. Paket yapısı (feature-by-module)

Her servis domain'leri **modül (feature)** bazında ayırır, katmanları modül içinde toplar:

```
<service>/
├── <module1>/
│   ├── controller/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── internal/         ← opsiyonel, iç (service-to-service) endpoint'ler
│   │   └── XxxController.java
│   ├── service/
│   │   ├── XxxService.java           (interface)
│   │   └── impl/
│   │       └── XxxServiceImpl.java
│   ├── entity/
│   │   └── Xxx.java
│   ├── repository/
│   │   └── XxxRepository.java
│   ├── mapper/
│   │   └── XxxMapper.java
│   ├── command/                      ← service layer'a geçen input record'ları
│   │   └── XxxCreateContext.java
│   ├── query/                        ← Redis cache'e yazılan *Info record'ları
│   │   └── XxxInfo.java
│   ├── constant/                     ← enum'lar, status'lar
│   └── exception/                    ← modüle özel exception'lar (opsiyonel)
├── <module2>/
│   └── ...
├── common/
│   ├── config/
│   │   └── CoreConfig.java
│   └── constants/
│       └── ApiPaths.java
├── client/                           ← dışarıya (başka servise) Feign
│   ├── XxxClient.java
│   ├── adapter/
│   │   └── XxxClientAdapter.java
│   ├── dto/
│   │   └── XxxClientResponse.java    ← sadece bu servisin ihtiyacı
│   └── config/
│       └── FeignConfig.java
├── outbox/
│   ├── entity/Outbox.java            ← extends BaseOutbox
│   ├── repository/OutboxRepository.java
│   ├── service/
│   │   ├── OutboxService.java
│   │   └── impl/OutboxServiceImpl.java
│   ├── config/OutboxConfig.java       ← @EnableScheduling (cleanup için)
│   └── scheduler/OutboxCleanupScheduler.java
├── inbox/
│   ├── entity/Inbox.java              ← extends BaseInbox
│   ├── repository/InboxRepository.java ← JpaRepository<Inbox, String>  (PK String!)
│   ├── service/...
│   ├── config/InboxConfig.java        ← @EnableScheduling + conditional
│   └── scheduler/InboxCleanupScheduler.java
└── XxxServiceApplication.java
```

Örnek referanslar: `product-service/src/main/java/com/ecommerce/productservice/product/...`, `stock-service/.../stock/...`.

### Bilinen sapmalar
- **user-tenant-service** `tenant/service/` altında `TenantLifecycleService`, `TenantMemberService` vb. interface+Impl pattern'ine uymuyor, **doğrudan `@Service`** ile tek sınıf. Bu bilerek yapıldı; bu servise dokunulurken aynı stili koru.
- **basket-service** modül bazlı değil, tek "basket" domain'i olduğu için `entity/`, `controller/`, `service/`, `repository/` direkt kökte.
- **payment-service** modül bazlı ama Outbox/Inbox `common-lib`'teki base'leri extend etmiyor — kendi tam tanımlı entity'leri var. Bu **aktif migration sırasında düzeltilecek** (Stage 4, bkz. `TODO.md`). Yeni kod yazarken diğer servislerin pattern'ini taban al.

## 2. Controller katmanı

### 2.1 Kural: Controller business logic içermez 👤

Controller sadece şunları yapar:
1. `@PreAuthorize(...)` ile yetki kontrolü
2. `@CurrentUser` ile user'ı al
3. Request DTO'yu Mapper ile `*Context` / `*Command` record'una çevir
4. Service'i çağır
5. Dönen entity veya `*Info`'yu Mapper ile Response DTO'ya çevir
6. `ResponseEntity` döndür

İf/else iş kuralları, koşullu DB sorguları, dış servis çağrıları **controller'da olmaz**.

İyi örnek — `TenantProductController.createProduct`:
```java
@PostMapping
@PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")
public ResponseEntity<ProductResponse> createProduct(
        @PathVariable Long tenantId,
        @Valid @RequestBody ProductCreateRequest request,
        @CurrentUser AuthUser user) {

    ProductCreateContext context = productMapper.toContext(request, tenantId, user.keycloakId());
    Product saved = tenantProductService.createProduct(context);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(productMapper.toResponse(saved));
}
```

### 2.2 URL sabitleri `ApiPaths` içinde

Controller'larda hardcode string yok. `common/constants/ApiPaths.java`:

```java
public class ApiPaths {
    private ApiPaths() {}

    public static final String BASE_PATH_V1 = "/api/v1";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TenantProduct {
        public static final String TENANT_PRODUCTS = BASE_PATH_V1 + "/products/tenants/{tenantId}";
    }
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Internal {
        public static final String INTERNAL_PRODUCTS = BASE_PATH_V1 + "/internal/products";
    }
}
```

### 2.3 URL namespace konvansiyonu

- `/api/v1/public/**` → auth gerektirmez (`GlobalSecurityConfig`'te permitAll)
- `/api/v1/categories/**` → auth gerektirmez (explicit permitAll)
- `/api/v1/internal/**` → sadece servisler arası; JWT var ama mantıksal ayrım
- Diğerleri → authenticated, genelde `@PreAuthorize(...)` ile ek tenant RBAC

### 2.4 `@CurrentUser AuthUser`

```java
public ResponseEntity<X> foo(@CurrentUser AuthUser user) {
    user.keycloakId();   // UUID — sub claim
    user.username();     // preferred_username
    user.email();
    user.firstName();
    user.lastName();
}
```
`user.keycloakId()` asla `null` olmamalı (authenticated endpoint'te). Public endpoint'te `AuthUser` null gelir, kullanma.

### 2.5 Tenant yetki kontrolü

```java
@PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")   // belirli rol
@PreAuthorize("@tenantSecurity.isMember(#tenantId)")           // sadece üye olmak yeter
```

Method parametresinin adı `tenantId` olmalı (SpEL `#tenantId`).

### 2.6 Idempotency

Non-idempotent POST'larda `@Idempotent`. Frontend her istek için benzersiz bir `Idempotency-Key` header'ı göndermeli.

```java
@Idempotent(cachePrefix = "idempotency:product-update:")
@PutMapping("/{productId}")
public ResponseEntity<...> updateProduct(...) { ... }
```

Aspect header yoksa 400 atar (`MISSING_IDEMPOTENCY_KEY`). Duplicate request'te 400 (`DUPLICATE_REQUEST`).

### 2.7 Page response dönüşümü

`PageResponse<Entity>` döndürme, servis `PageResponse<Info>` döner:
```java
PageResponse<ProductInfo> result = tenantProductService.getTenantProducts(tenantId, page, size);
PageResponse<ProductResponse> response = new PageResponse<>(
        result.content().stream().map(productMapper::toResponseFromInfo).toList(),
        result.pageNumber(), result.pageSize(),
        result.totalElements(), result.totalPages(), result.isLast()
);
```

Alternatif olarak Spring Data `Page<>` direkt döndürülüyor (payment-service history, search-service search). `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` açıldığı için serialize olur. **Yeni kodda `PageResponse` tercih et** — stabil sözleşme.

## 3. Service katmanı

### 3.1 Kural: Service DTO almaz, DTO dönmez 👤

Service interface'i:
- Parametre olarak alır: **entity, primitive/wrapper, `*Command`/`*Context` record**
- Döner: **entity veya `*Info` record (query paketinden)**

`Request`/`Response` tipleri `controller/dto/` dışına çıkmaz. Bunları service imzasında görürsen düzelt.

İyi örnek:
```java
public interface TenantProductService {
    Product createProduct(ProductCreateContext context);
    Product updateProduct(Long tenantId, Long productId, ProductUpdateContext context);
    ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId);
    PageResponse<ProductInfo> getTenantProducts(Long tenantId, int page, int size);
}
```

### 3.2 Command/Context vs. Info record'ları

- **`*Context` / `*Command` record** — `command/` paketinde. Controller'dan service'e geçen input. İmmutable. Body + path id'ler + auth user id birleşiminden oluşur.

  ```java
  public record ProductCreateContext(
          Long tenantId,
          UUID keycloakId,
          Long categoryId,
          ...
  ) {}
  ```

- **`*Info` record** — `query/` paketinde. Service'in döndürdüğü read-only snapshot. **`Serializable` implement eder** (Redis cache'e JDK serialization ile yazılabilsin diye).

  ```java
  public record ProductInfo(
          Long id,
          Long tenantId,
          ...
  ) implements Serializable {}
  ```

### 3.3 Transaction sınırları

- Mutasyon: `@Transactional`
- Read: `@Transactional(readOnly = true)`
- Outbox publish metotları: `@Transactional(propagation = Propagation.MANDATORY)` — **dış transaction zorunlu**. Tek başına çağrılamaz; iş transaction'ı içinde aynı atomic unit'in parçası olur.
- `IyzicoTransactionService.save`: `@Transactional(propagation = REQUIRES_NEW)` — ana ödeme rollback olsa bile log kalsın.
- `InboxServiceImpl.isMessageProcessed`: `@Transactional(propagation = REQUIRES_NEW)` — PK çakışması fırlattığında ana consumer tx'ini rollback etmesin.

### 3.4 Cache

`@Cacheable` ve `@CacheEvict` service katmanında; cache name'ler service'e özel:

```java
@Cacheable(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId")
public ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId) { ... }

@Caching(evict = {
    @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId"),
    @CacheEvict(cacheNames = "public-product", key = "#productId")
})
public Product updateProduct(Long tenantId, Long productId, ProductUpdateContext context) { ... }
```

**Önemli:** `@Cacheable`'lı metot `*Info` döndürmeli, entity değil. Entity yazarsan Hibernate lazy proxy ve `LazyInitializationException` ile başın ağrır.

Cache isim listesi için `ARCHITECTURE.md` §6.

## 4. Entity / Repository

### 4.1 BaseEntity

```java
@Entity
@Table(name = "products")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Product extends BaseEntity {
    // BaseEntity: id (Long, IDENTITY), createdAt, updatedAt
    ...
}
```

`@PrePersist` / `@PreUpdate` BaseEntity'de tanımlı.

### 4.2 Enum ve JSON

```java
@Enumerated(EnumType.STRING)                    // ALWAYS STRING, ordinal kullanma
private ProductStatus status = ProductStatus.ACTIVE;

@JdbcTypeCode(SqlTypes.JSON)                    // Postgres jsonb mapping
@Column(columnDefinition = "jsonb")
private Map<String, String> attributes;
```

### 4.3 Rich domain (örn. Stock)

Entity iş kurallarını barındırabilir:
```java
public class Stock extends BaseEntity {
    public void reserve(int amount) {
        if (amount <= 0) throw new BusinessException("Miktar 0'dan büyük olmalı!", "INVALID_AMOUNT");
        if (this.availableQuantity < amount)
            throw new BusinessException("Yetersiz stok!", "OUT_OF_STOCK");
        this.availableQuantity -= amount;
        this.reservedQuantity += amount;
    }
    ...
}
```

Service sadece `stock.reserve(amount)` çağırır. Yeni business-heavy entity'ler yazarken tercih edilen stil.

### 4.4 Repository

Spring Data JPA interface'leri. Custom query'de JPQL `@Query`, fetch join gerekiyorsa `LEFT JOIN FETCH`.

Lock kullanımı örneği (stock-service):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
Optional<Stock> findWithLockingByTenantIdAndWarehouseIdAndProductId(...);
```

> **Inbox repository tuzağı:** `public interface InboxRepository extends JpaRepository<Inbox, String>` — PK **`String`**, Long değil. `BaseInbox.messageId` `@Id VARCHAR`. Yanlış tip yazarsan Spring Data proxy hata vermeden bean oluşturur ama runtime'da sorgularda patlarsın.

### 4.5 Soft delete

DB'den row silme yerine `status = DELETED` veya `isActive = false`. Örnek:
```java
product.setStatus(ProductStatus.DELETED);
product.setSalesStatus(SalesStatus.OUT_OF_STOCK);
productRepository.save(product);
outboxService.publishProductDeletedEvent(product);
```

Query'lerde `status != DELETED` filtresi (`findAllByTenantIdAndStatusNot`).

### 4.6 Flyway migration kuralları ⚠️

- Dizin: `src/main/resources/db/migration/`
- İsim: `V{N}__aciklama.sql` (çift underscore!)
- **Mevcut V1, V2... dosyalarını ASLA düzenleme.** Flyway checksum kontrolü yapar, bozulur.
- Schema değişikliği = yeni `V(N+1)` dosyası. İçeriği idempotent yap (`IF NOT EXISTS`, `DROP IF EXISTS`).
- `spring.jpa.hibernate.ddl-auto: validate` her yerde. Migration çalışmazsa boot edemez.
- **Dev ortamı:** Checksum çakışması olursa, volume'ü silip sıfırdan başlatmak serbest: `docker compose down -v` ve ilgili `infrastructure/postgres_data` sil. Prod olsa dert olurdu, şu an değil.

### 4.7 Shared Schema multi-tenancy

Tüm tenant'lar aynı tabloları paylaşır, `tenant_id BIGINT NOT NULL` kolonu ayrıştırır. Schema-per-tenant veya DB-per-tenant yok.

Kural: Tenant-scoped entity için **her query'de `tenant_id` filter'ı zorunlu**. Repository metotları `findByIdAndTenantId`, `findAllByTenantId` gibi. Sadece `findById` kullanma — tenant sınırını aşarsın.

## 5. Mapper (MapStruct)

```java
@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductMapper {
    ProductCreateContext toContext(ProductCreateRequest request, Long tenantId, UUID userId);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)    // service manuel set ediyor
    @Mapping(target = "status", constant = "ACTIVE")
    Product toEntity(ProductCreateContext context);

    @Mapping(source = "category.id", target = "categoryId")
    ProductResponse toResponse(Product product);

    ProductResponse toResponseFromInfo(ProductInfo info);
    ProductResponse toResponseFromPublicInfo(PublicProductInfo info);
}
```

İstisnai dönüşümler için `default` metot (bkz. `PaymentMapper.toPaymentHistoryResponse`). MapStruct 1.6.3 processor `pom.xml`'de annotation processor olarak tanımlı.

## 6. Exception handling

Beş exception tipi, hepsi `common-lib`'te:

| Exception | HTTP | Ne zaman |
|---|---|---|
| `BusinessException` | 400 | İş kuralı ihlali. `errorCode` + opsiyonel `Map<String,Object> details`. |
| `ResourceNotFoundException` | 404 | Entity/kaynak bulunamadı. |
| `ExternalServiceException` | 503 | Dış servise (başka microservice, iyzico, vs.) ulaşılamadı. |
| `SystemException` | (500, handler'ı yok) | Teknik hata — düzgün handle edilmiyor, yakalanmayan exception olarak 500 döner. |
| `MethodArgumentNotValidException` | 400 | Bean validation — `errorCode: VALIDATION_ERROR` ile map döner. |

Her custom exception constructor'ı `(String message, String errorCode)` alır. `throw new BusinessException("Mesaj", "ERROR_CODE")`.

`GlobalExceptionHandler` (common-lib) `@RestControllerAdvice` olarak tüm servislere yayılır; servis özelinde `@ControllerAdvice` ekleme (user-tenant-service'te `UserExceptionHandler` var — sapma).

## 7. Outbox / Event publishing

### 7.1 Yeni event tipi ekleme

1. **`event-contracts`'e payload record'u ekle**:
   ```java
   // backend/event-contracts/src/main/java/com/ecommerce/contracts/event/product/ProductXxxEventPayload.java
   public record ProductXxxEventPayload(Long productId, Long tenantId, String someField) {}
   ```
2. **`common-lib` `EventConstants`'a sabit ekle**:
   ```java
   public static final String EVENT_PRODUCT_XXX = "PRODUCT_XXX_EVENT";
   ```
3. **Producer service'te `OutboxService` metodu**:
   ```java
   @Override
   @Transactional(propagation = Propagation.MANDATORY)
   public void publishProductXxxEvent(Product product) {
       try {
           var payload = new ProductXxxEventPayload(product.getId(), product.getTenantId(), ...);
           Outbox outbox = Outbox.builder()
                   .aggregateType(EventConstants.AGGREGATE_PRODUCT)
                   .aggregateId(product.getId().toString())
                   .messageType(EventConstants.EVENT_PRODUCT_XXX)
                   .messagePayload(objectMapper.writeValueAsString(payload))
                   .build();
           outboxRepository.save(outbox);
       } catch (JsonProcessingException e) {
           throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
       }
   }
   ```
4. **Consumer service'te `@KafkaListener`**:
   ```java
   case EventConstants.EVENT_PRODUCT_XXX -> handleProductXxx(unescapedJson);
   ```
5. **event-contracts'i breaking change'siz** güncelle — sadece yeni field ekle, mevcutları silme/yeniden adlandırma.

### 7.2 Kafka message header okuma

```java
@KafkaListener(topics = EventConstants.AGGREGATE_PRODUCT, groupId = "search-service-group")
public void consume(
        String messagePayload,
        @Header(value = "message_type", required = false) String headerEventType) {
    // Debezium JSONB'yi string olarak yayar, bir kez daha unwrap:
    String unescapedJson = objectMapper.readValue(messagePayload, String.class);
    MyPayload p = objectMapper.readValue(unescapedJson, MyPayload.class);
}
```

**Header adı `message_type`** (Debezium config'de placement `message_type:header:message_type`). `eventType` değil.

## 8. Feign client ekleme

```java
// 1. Client interface
@FeignClient(name = "<hedef-service>", url = "${application.clients.<hedef>.url}")
public interface XxxClient {
    @GetMapping("/api/v1/...")
    XxxClientResponse getSomething(@PathVariable Long id);
}

// 2. DTO — @JsonIgnoreProperties ile kısmi DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public record XxxClientResponse(Long id, String name, ...) {}

// 3. Adapter
@Component
@RequiredArgsConstructor
@Slf4j
public class XxxClientAdapter {
    private final XxxClient xxxClient;
    public XxxClientResponse validateAndGet(Long id) {
        try {
            return xxxClient.getSomething(id);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Bulunamadı", "X_NOT_FOUND");
        } catch (FeignException e) {
            throw new ExternalServiceException("Servise ulaşılamadı", "X_SERVICE_DOWN");
        }
    }
}

// 4. config/FeignConfig.java (bir kez; bu servisteki tüm clientlar için):
@Configuration
@EnableFeignClients(basePackages = {"com.ecommerce.<this-service>", "com.ecommerce.common"})
public class FeignConfig {}
```

application.yml:
```yaml
application:
  clients:
    <hedef>:
      url: ${<HEDEF>_SERVICE_URL}
```

JWT otomatik forward edilir (`FeignClientInterceptor` common-lib'te, component scan ile gelir).

> **Tuzak:** Test'te bu env set edilmezse context load fail. `@TestPropertySource` veya `application-test.yml` ile bir default ver.

## 9. Redis kullanımı

### 9.1 Cache olarak (varsayılan)

```java
@Cacheable(cacheNames = "my-cache", key = "#id")
public MyInfo getInfo(Long id) { ... }   // MyInfo Serializable record

@CacheEvict(cacheNames = "my-cache", key = "#id")
public void updateSomething(Long id, ...) { ... }
```

`application.yml`:
```yaml
spring:
  cache: { type: redis }
  data:
    redis: { host: localhost, port: 6379, password: ${REDIS_PASSWORD} }
```

Kural: `@Cacheable` dönüşü `*Info` record (`Serializable`). Entity veya Response DTO cache'leme 👤.

### 9.2 Redis'i primary store olarak

Sadece `basket-service`:
```java
@RedisHash(value = "Basket", timeToLive = 604800)
public class Basket {
    @Id private UUID userId;
    private List<BasketItem> items;
}
public interface BasketRepository extends CrudRepository<Basket, String> {}
```
Cache değil, tek source of truth. DB yok.

### 9.3 Distributed lock (Redisson)

```java
RLock lock = redissonClient.getLock("lock:basket:" + userId);
if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
    try { /* critical section */ } finally {
        if (lock.isHeldByCurrentThread()) lock.unlock();
    }
}
```

### 9.4 Manuel Redis (StringRedisTemplate)

Authz cache gibi "native" kullanım için `StringRedisTemplate`:
```java
redisTemplate.opsForHash().put("authz:user:" + userId, "tenant_" + tenantId, role);
redisTemplate.expire("authz:user:" + userId, Duration.ofHours(2));
```

## 10. Yetki invalidation

Bir kullanıcının tenant rolü değiştiğinde cache'i temizle:
```java
authzCacheService.evictUserCache(user.getKeycloakId());
```

Unutulursa kullanıcı eski rolüyle 2 saat boyunca yetki kontrolünden geçer.

## 11. Application.yml standart yapısı

Her servisin iki dosyası: `application.yml` + `application-dev.yml`:

```yaml
# application.yml
spring:
  application: { name: my-service }
  profiles: { active: dev }
  config: { import: optional:file:.env[.properties] }
  jpa:
    hibernate: { ddl-auto: validate }
    open-in-view: false
    properties: { hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect }
  cache: { type: redis }          # kullanıyorsa

jwt:
  auth:
    converter:
      resource-id: ${KEYCLOAK_CLIENT_ID}
      principal-attribute: preferred_username

management:
  endpoints: { web.exposure.include: "health, info, prometheus, metrics" }
  prometheus: { metrics.export.enabled: true }
  tracing: { sampling.probability: 1.0 }
  zipkin: { tracing.endpoint: http://localhost:9411/api/v2/spans }

application:
  clients:
    user-tenant: { url: ${USER_TENANT_SERVICE_URL} }

# application-dev.yml
server: { port: 80XX }
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/${X_DB_NAME}
    username: ${X_DB_USERNAME}
    password: ${X_DB_PASSWORD}
  kafka:
    producer: { bootstrap-servers: localhost:29092 }
  data:
    redis: { host: localhost, port: 6379, password: ${REDIS_PASSWORD} }

security:
  oauth2:
    resource-server:
      jwt: { issuer-uri: http://localhost:8080/realms/${REALM_NAME} }
```

> Bazı mevcut yml'lerde `resource server` (boşluklu) yazımı var; Spring Boot relaxed binding'den dolayı çalışıyor ama **`resource-server`** doğru yazım. Yeni kodda doğru yaz.

## 12. Logging

- `@Slf4j` kullan
- Türkçe/İngilizce mesajlar karışık — çevredeki stile uy
- **Hassas veri loglanmaz**: kart numarası, CVC, full JWT, şifre. (payment-service'te `System.out.println(cardInfo)` var, bu bug, düzeltilecek — TODO)
- `GeneralLoggerAspect` (common-lib) tüm service metotlarını otomatik loglar (`Start -> Method: X, Args: ...` + `End -> ...`). **payment-service bu aspect'ten hariç tutulmuş** (pointcut'ta `!within(com.ecommerce.paymentservice..*)`) — kart bilgisi sızmasın diye. payment-service'te metot girişini elle logla ama Args yazma.

## 13. Testing

Şu an sadece **stock-service**'te anlamlı test var. Pattern:

### Unit test (domain)
```java
class StockDomainTest {
    @Test
    @DisplayName("Stok yeterliyse rezervasyon başarılı olmalı")
    void reserve_ok() {
        Stock s = Stock.builder().availableQuantity(10).build();
        s.reserve(3);
        assertThat(s.getAvailableQuantity()).isEqualTo(7);
    }
}
```

### Integration test (Testcontainers)
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractBaseIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    @Container @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    @MockitoBean private JwtDecoder jwtDecoder;
}
```

### Feign + WireMock
```java
@AutoConfigureWireMock(port = 0)
@TestPropertySource(properties = {"application.clients.product.url=http://localhost:${wiremock.server.port}"})
class ProductClientAdapterIT extends AbstractBaseIntegrationTest { ... }
```

### Pessimistic lock concurrent test
`StockRepositoryIT` — 10 thread aynı stoğu rezerve etmeye çalışırken sadece birinin başarılı olmasını doğrular.

Yeni test yazmadan önce `stock-service/src/test/`'e bak.

## 14. Türkçe/İngilizce karma nerede?

- **Exception error code'ları**: İngilizce UPPER_SNAKE (`PRODUCT_NOT_FOUND`, `DUPLICATE_REQUEST`) — tutarlı
- **Exception mesajları**: Türkçe (frontend kullanıcıya gösterir)
- **Log mesajları**: Türkçe yaygın, İngilizce de kabul
- **Kod (değişken, metot, sınıf)**: İngilizce — istisna yok
- **TODO yorumları**: Türkçe + tarih + initial (`// TODO [DD.MM.YYYY HH:MM]: ...`) — bu pattern sıkı, koru

## 15. Bilinen sapma/kalıp özeti

Aktif migration sürecindeki item'lar `TODO.md` "Stage 3/4" altında. Aşağıdakiler **kalıcı veya yapısal** sapmalar:

| Konum | Beklenen | Gerçek | Not |
|---|---|---|---|
| `user-tenant-service/tenant/service/*` | `interface + impl` | doğrudan concrete `@Service` | Bilinçli yapıldı, dokunulduğunda koru |
| `user-tenant-service/user/exception/UserExceptionHandler.java` | yok (common-lib'teki GlobalExceptionHandler yeterli) | servis özel advice | Yedek handler; gerekirse temizlenebilir |
| `search-service` consumer Inbox idempotency | var | kullanılmıyor (şema ve helper lib'de, çağıran yok) | TODO — teknik borç |
| payment-service `GeneralLoggerAspect`'ten hariç | — | pointcut'ta exclude | Bilinçli, hassas veri için; koru |
| api-gateway `application-prod.yml` | tüm route'lar | sadece user-tenant-service | Prod yok, eksik önemsiz |
| `application.yml`'de `resource server` (boşluklu) | `resource-server` | bazı servislerde yanlış | Küçük, dokunulduğunda düzelt |

**Aktif migration sırasında düzeltilecekler** (bunlar sapma değil, yapılacak iş):

| Konum | Durum | Stage |
|---|---|---|
| payment-service Outbox/Inbox base extend etmiyor | `BaseOutbox`/`BaseInbox`/`common.InboxStatus`'a migrate | 4 |
| payment-service `pom.xml` `event-contracts` yok | publish senaryosu kararlaşınca ekle | 4 |
| user-tenant-service Outbox entity BaseOutbox extend etmiyor | migrate | 4 |
| user-tenant-service `pom.xml` `event-contracts` yok | publish senaryosu kararlaşınca ekle | 4 |

## 16. Tuzaklar (geçmişte takıldığımız yerler)

Kullanıcı bu noktalarda zaman harcadı, aynı şeye tekrar takılma:

### 16.1 Debezium
- **EventRouter default'ta `type` kolonu arar.** Kolon adın `message_type` veya başka bir şeyse `transforms.outbox.table.fields.additional.placement` **explicit** verilmek zorunda. Yoksa Debezium sessizce çalışmaz — log'da hata yazmaz, sadece mesaj gitmez. Bizim config: `message_type:header:message_type`.
- **Replication slot type migration.** Outbox id tipi UUID'den BIGINT'e geçilince (stock-service `V3` gibi), Debezium'un tuttuğu replication slot eski şemayı hatırlar. Connector'ı silip yeniden push yetmez — **slot'u da drop etmek gerekir**:
  ```sql
  SELECT pg_drop_replication_slot('stock_service_debezium_slot');
  ```
  Sonra connector re-register (slot Debezium tarafından otomatik yeniden oluşturulur).
- **`REPLICA IDENTITY FULL`** outbox tablosunda yoksa, DELETE ve UPDATE event'leri eksik satır bilgisi ile gelir. Yeni outbox tablosu yaratırken:
  ```sql
  ALTER TABLE outbox REPLICA IDENTITY FULL;
  ```

### 16.2 Lombok
- **`@Builder` vs `@SuperBuilder` çakışması.** Parent class `@SuperBuilder` ise (ör. `BaseOutbox`, `BaseInbox`), subclass da `@SuperBuilder` olmalı — `@Builder` koyarsan parent field'ları include edilmez, runtime'da eksik kaydedilir. Unutma:
  ```java
  @SuperBuilder   // NOT @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public class Outbox extends BaseOutbox { ... }
  ```

### 16.3 Spring Data JPA
- **`JpaRepository<Inbox, String>`** — `BaseInbox.messageId` `@Id VARCHAR`. ID tipi `String`, `Long` değil. Yanlış tip → bean oluşur ama sorgularda hata.
- **Repository interface metodu parametre tipi** entity'deki alan tipiyle eşleşmeli (`findByTenantIdAndWarehouseIdAndProductId` — hepsi `Long`).

### 16.4 Feign test ortamı
- `@FeignClient(url = "${application.clients.X.url}")` — env yoksa context load fail. Test'te `@TestPropertySource(properties = {"application.clients.X.url=http://localhost:1234"})` ver, veya `application-test.yml`'de set et. Prod URL'lerini test'te kullanma.

### 16.5 `envsubst` ve Debezium JSON
- `register_connector.sh`'ta `envsubst` kullanıyorsak connector JSON'ındaki `${routedByValue}` gibi Debezium'un kendi placeholder'larını expand etmemesi için **whitelist** kullan:
  ```bash
  envsubst '${POSTGRES_USER} ${POSTGRES_PASSWORD}' < file.json | curl ...
  ```
  Aksi halde `${routedByValue}` boşalır ve connector broken gelir.

### 16.6 `@Transactional` self-invocation
- Aynı sınıf içinde `this.methodA()` çağrısı proxy üzerinden geçmez, `@Transactional` devreye girmez. Spring AOP proxy'leri **external çağrılarda** çalışır. Çözüm: metodu ayrı bean'e al, veya `AopContext.currentProxy()` kullan.
- Örnek: `SubscriptionRenewalService.processDailyRenewals` → `processSingleRenewal` — şu an aynı sınıf, `@Transactional` çalışmıyor. TODO.

### 16.7 Hibernate lazy loading + cache
- `@Cacheable` bir metot **entity** döndürürse, Redis serialize etmeye çalışırken lazy proxy'ye dokunur, `LazyInitializationException` fırlatır. **Her zaman `*Info` record döndür** `@Cacheable`'lı metotlardan.
