---
name: spring-service-conventions
description: Use when writing or reviewing Java Spring Boot code in any backend service. Covers the layered architecture (controller/service/repository/entity/mapper), DTO/Command/Info/Request/Response distinctions, @Transactional boundaries, @Cacheable rules, exception handling, tenant RBAC, @CurrentUser, Feign client patterns, and MapStruct. Triggers on controller/service/entity/mapper work.
---

# Spring Servis Katman Kuralları

Bu skill kullanıcının kesin kurallarını içerir (👤 işaretli). **CONVENTIONS.md** ile birlikte çalışır — oradaki detaylar burada özet + pratik kullanım olarak. Çelişki olursa CONVENTIONS.md baz alınır.

## En önemli iki kural (ihlal etme)

### 1. Service katmanı DTO kabul etmez, DTO dönmez 👤

Service interface'i:
- **Alır:** entity, primitive/wrapper, `*Command` veya `*Context` record
- **Döner:** entity veya `*Info` record (query/ paketinde, `Serializable`)

**Yanlış (görülünce düzelt):**
```java
public Product createProduct(ProductCreateRequest request) { ... }    // ← Request geçmez
public ProductResponse getProduct(Long id) { ... }                    // ← Response dönmez
```

**Doğru:**
```java
public Product createProduct(ProductCreateContext context) { ... }
public ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId) { ... }
```

### 2. Controller business logic içermez 👤

Controller sadece 6 şey yapar:
1. `@PreAuthorize` ile yetki kontrolü
2. `@CurrentUser` ile user'ı al
3. Request DTO'yu Mapper ile `*Context`/`*Command`'a çevir
4. Service'i çağır
5. Dönen entity/Info'yu Mapper ile Response DTO'ya çevir
6. `ResponseEntity` döndür

**If/else iş kuralları, koşullu DB sorguları, dış servis çağrıları → service'e aittir.**

Tipik controller:
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

## Record tipleri

| Tip | Paket | Role | Serializable |
|---|---|---|---|
| `*Request` | `controller/dto/request/` | Controller input (JSON body) | — |
| `*Response` | `controller/dto/response/` | Controller output | — |
| `*Context` / `*Command` | `command/` | Service input (immutable) | — |
| `*Info` | `query/` | Service output, Redis'e yazılabilir read-only snapshot | **Evet** |

### `*Context` örneği

Body + path id'ler + auth user id birleşimi:

```java
public record ProductCreateContext(
        Long tenantId,           // PathVariable
        UUID keycloakId,         // @CurrentUser
        Long categoryId,         // body
        String name,             // body
        BigDecimal price,        // body
        ...
) {}
```

Controller mapper ile oluşturur, service'e geçer. Immutable.

### `*Info` örneği

Entity'den türetilmiş read-only snapshot. **`Serializable` implement eder** (Redis JDK serialization için):

```java
public record ProductInfo(
        Long id,
        Long tenantId,
        Long categoryId,
        String name,
        BigDecimal price,
        ...
) implements Serializable {}
```

`@Cacheable`'lı metotlar `*Info` döndürür, entity **döndürmez** — Hibernate lazy proxy'den `LazyInitializationException` yersin.

## Transaction sınırları

```java
// Mutasyon
@Transactional
public Product createProduct(...) { ... }

// Read (çoklu sorgu veya lazy field erişimi varsa)
@Transactional(readOnly = true)
public ProductInfo getInfo(...) { ... }

// Outbox publish — dış tx zorunlu
@Transactional(propagation = Propagation.MANDATORY)
public void publishProductCreatedEvent(Product p) { ... }

// Bağımsız log/audit — ana işlem rollback olsa da log kalsın
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveTransactionLog(...) { ... }

// Inbox idempotency check — PK çakışma consumer tx'ini bozmasın
@Transactional(propagation = Propagation.REQUIRES_NEW)
public boolean isMessageProcessed(...) { ... }
```

**Self-invocation tuzağı:** aynı sınıftaki metot çağrısı proxy'den geçmez, `@Transactional` çalışmaz. Çözüm: ayrı bean'e al veya `AopContext.currentProxy()`.

## @Cacheable kuralları

```java
@Cacheable(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId")
public ProductInfo getProductInfoByIdAndTenantId(Long productId, Long tenantId) { ... }

@Caching(evict = {
    @CacheEvict(cacheNames = "tenant-product", key = "#tenantId + ':' + #productId"),
    @CacheEvict(cacheNames = "public-product", key = "#productId")
})
public Product updateProduct(...) { ... }
```

Kurallar:
- Dönüş **`*Info`** (Serializable record) — entity dönme
- Mutasyon metodunda evict **hem tenant-scoped hem public cache'i**
- Cache key pattern cache ismi ile tutarlı

Redis key pattern haritası: `ARCHITECTURE.md §6`.

## Exception handling

5 exception tipi, hepsi `common-lib`:

| Exception | HTTP | Kullanım |
|---|---|---|
| `BusinessException` | 400 | İş kuralı ihlali (stok yok, sku çakışması vb.) |
| `ResourceNotFoundException` | 404 | Entity bulunamadı |
| `ExternalServiceException` | 503 | Başka servis / iyzico erişilemez |
| `SystemException` | 500 | Teknik hata (JSON parse, IO) |
| `MethodArgumentNotValidException` | 400 | Bean validation (`@Valid`) |

Her custom exception `(String message, String errorCode)` alır:

```java
throw new BusinessException("Yetersiz stok!", "OUT_OF_STOCK");
throw new ResourceNotFoundException("Ürün bulunamadı", "PRODUCT_NOT_FOUND");
```

- **`errorCode`** her zaman `UPPER_SNAKE` İngilizce
- **`message`** Türkçe (frontend kullanıcıya gösterir)

## Tenant RBAC

Her tenant-scoped controller endpoint'i:

```java
@PreAuthorize("@tenantSecurity.hasRole(#tenantId, 'OWNER')")    // belirli rol
@PreAuthorize("@tenantSecurity.isMember(#tenantId)")            // sadece üye olmak yeter
```

Kurallar:
- Method parametresinin adı **`tenantId`** olmalı (SpEL `#tenantId`)
- `'OWNER'` / `'ADMIN'` / `'STAFF'` / `'ACCOUNTANT'` rolleri var
- Rol/üyelik değişirse `authzCacheService.evictUserCache(keycloakId)` çağrılmalı, yoksa 2 saat eski rol kalır

Repository sorularında:
- Her tenant-scoped entity için `findByIdAndTenantId`, `findAllByTenantId` kullan
- `findById` **tek başına kullanma** — tenant sınırını aşar, veri sızar

## @CurrentUser

```java
public ResponseEntity<X> foo(@CurrentUser AuthUser user) {
    user.keycloakId();   // UUID — JWT sub claim
    user.username();
    user.email();
    user.firstName();
    user.lastName();
}
```

- Authenticated endpoint'te `user` null olmaz
- Public endpoint'te `AuthUser` null gelir, kullanma
- `keycloakId()` → UUID, DB FK'larında `created_by_user_id UUID` kolonuna yazılır

## Feign client ekleme (4 dosya)

### 1. Client interface
```java
@FeignClient(name = "product-service", url = "${application.clients.product.url}")
public interface ProductClient {
    @GetMapping("/api/v1/products/tenants/{tenantId}/{productId}")
    ProductClientResponse getTenantProduct(@PathVariable Long tenantId, @PathVariable Long productId);
}
```

### 2. DTO — sadece bu servisin ihtiyaç duyduğu field'lar
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductClientResponse(Long id, String name, BigDecimal price) {}
```

### 3. Adapter — FeignException'ı domain exception'a çevirir
```java
@Component @RequiredArgsConstructor
public class ProductClientAdapter {
    private final ProductClient productClient;

    public ProductClientResponse validateAndGetProduct(Long productId, Long tenantId) {
        try {
            return productClient.getTenantProduct(tenantId, productId);
        } catch (FeignException.NotFound | FeignException.BadRequest e) {
            throw new BusinessException("Bu ürün size ait değil veya bulunamadı!", "PRODUCT_NOT_OWNED");
        } catch (Exception e) {
            throw new ExternalServiceException("Ürün doğrulaması yapılamadı", "PRODUCT_SERVICE_DOWN");
        }
    }
}
```

### 4. application.yml
```yaml
application:
  clients:
    product:
      url: ${PRODUCT_SERVICE_URL}
```

Service katmanı **adapter**'ı inject eder, client'ı direkt kullanmaz.

## MapStruct pattern

```java
@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductMapper {
    // Request → Context
    ProductCreateContext toContext(ProductCreateRequest request, Long tenantId, UUID userId);

    // Context → Entity (ignore'lar manuel)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)    // service manuel set eder
    @Mapping(target = "status", constant = "ACTIVE")
    Product toEntity(ProductCreateContext context);

    // Entity → Response
    @Mapping(source = "category.id", target = "categoryId")
    ProductResponse toResponse(Product product);

    // Info → Response (iki ayrı dönüşüm yolu)
    ProductResponse toResponseFromInfo(ProductInfo info);
}
```

İstisnai dönüşümler için `default` metot. MapStruct annotation processor `pom.xml`'de.

## Soft delete

```java
product.setStatus(ProductStatus.DELETED);
product.setSalesStatus(SalesStatus.OUT_OF_STOCK);
productRepository.save(product);
outboxService.publishProductDeletedEvent(product);
```

Query'lerde filter: `findAllByTenantIdAndStatusNot(tenantId, DELETED, ...)`.

## Rich domain — entity iş kurallarını barındırabilir

```java
public class Stock extends BaseEntity {
    public void reserve(int amount) {
        if (amount <= 0) throw new BusinessException("Miktar 0'dan büyük olmalı!", "INVALID_AMOUNT");
        if (this.availableQuantity < amount)
            throw new BusinessException("Yetersiz stok!", "OUT_OF_STOCK");
        this.availableQuantity -= amount;
        this.reservedQuantity += amount;
    }
}
```

Service: `stock.reserve(amount);`. Bu stil yeni business-heavy entity'lerde tercih edilir.

## Idempotency (non-idempotent POST)

```java
@Idempotent(cachePrefix = "idempotency:product-update:")
@PutMapping("/{productId}")
public ResponseEntity<...> updateProduct(...) { ... }
```

- Frontend her istek için benzersiz `Idempotency-Key` header'ı gönderir
- Header yoksa 400 `MISSING_IDEMPOTENCY_KEY`
- Duplicate request'te 400 `DUPLICATE_REQUEST`
- TTL default 24h

## URL namespace

- `/api/v1/public/**` → permitAll
- `/api/v1/categories/**` → permitAll
- `/api/v1/internal/**` → servisler arası (JWT var ama mantıksal ayrım)
- Diğerleri → authenticated + `@PreAuthorize`

Sabitler `common/constants/ApiPaths.java`'da — hardcode string yok.

## Bilinen sapmalar (dokunulunca koru)

- `user-tenant-service/tenant/service/*` — interface+impl değil, doğrudan `@Service`. Bilinçli.
- `basket-service` — modül bazlı değil, düz yapı. Redis primary store olduğu için.
- `payment-service/GeneralLoggerAspect`'ten hariç — kart bilgisi sızmasın diye pointcut'ta exclude. Koru.
- Bazı `application.yml`'de `resource server` (boşluklu) — yanlış ama Spring relaxed binding ile çalışıyor. Dokununca `resource-server` olarak düzelt.

## İlişkili dosyalar

- Detay: `CONVENTIONS.md` (§1-§16)
- Mimari: `ARCHITECTURE.md`
- Outbox: `outbox-inbox-pattern` skill
- Migration: `flyway-migration-rules` skill
- MinIO: `minio-storage-pattern` skill (sonraki turda)
