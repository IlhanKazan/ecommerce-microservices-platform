---
name: outbox-inbox-pattern
description: Use when working with event publishing (outbox), event consuming (inbox), BaseOutbox/BaseInbox entities, OutboxService/InboxService, Kafka consumers, event-contracts payloads, or @KafkaListener. Covers the transactional outbox pattern, @SuperBuilder tuzağı, @Transactional propagation rules, and Kafka header-based event dispatch.
---

# Outbox/Inbox Pattern Rehberi

Bu skill IlhanKazan E-Commerce platformunda event publish/consume yapılırken uyulması gereken pattern'leri, tuzakları ve tarifi içerir. **ARCHITECTURE.md §3** ve **CONVENTIONS.md §7**'nin genişletilmiş hali — onları tamamlar.

## Temel kural: Kafka Producer DOĞRUDAN kullanılmaz

Servis iş verisi yazıp aynı anda Kafka'ya event publish etmeye kalkmaz. Bunun yerine:

1. İş transaction'ı içinde `outbox` tablosuna bir satır INSERT edilir.
2. Debezium CDC ile bu satır okunur, Kafka topic'ine çevrilir.
3. Consumer topic'i dinler.

Bu **transactional outbox pattern** — iş verisi + event kaydı aynı DB transaction'ında, atomicity garantili.

## Producer tarafı

### 1. Yeni event eklemek için checklist

Yeni bir event tipi publish edecekse şu sıralamaya uy:

1. **`event-contracts`'e payload record'u ekle** (`backend/event-contracts/src/main/java/com/ecommerce/contracts/event/<domain>/`)
   ```java
   public record OrderCreatedEventPayload(Long orderId, Long tenantId, BigDecimal total) {}
   ```
   Record zero-dep pure Java olmalı. Lombok yok, Spring yok.

2. **`common-lib/EventConstants`'a sabit ekle:**
   ```java
   public static final String AGGREGATE_ORDER = "ORDER";
   public static final String EVENT_ORDER_CREATED = "ORDER_CREATED_EVENT";
   ```

3. **Producer servisinde `OutboxService` interface metodu + impl yaz:**
   ```java
   // OutboxService.java
   void publishOrderCreatedEvent(Order order);

   // OutboxServiceImpl.java
   @Override
   @Transactional(propagation = Propagation.MANDATORY)
   public void publishOrderCreatedEvent(Order order) {
       try {
           var payload = new OrderCreatedEventPayload(order.getId(), order.getTenantId(), order.getTotal());
           Outbox outbox = Outbox.builder()
                   .aggregateType(EventConstants.AGGREGATE_ORDER)
                   .aggregateId(order.getId().toString())
                   .messageType(EventConstants.EVENT_ORDER_CREATED)
                   .messagePayload(objectMapper.writeValueAsString(payload))
                   .build();
           outboxRepository.save(outbox);
       } catch (JsonProcessingException e) {
           throw new SystemException("Event JSON parse hatası", "JSON_PROCESSING_ERROR");
       }
   }
   ```

4. **İş service'inden çağır** — aynı `@Transactional` içinde:
   ```java
   @Transactional
   public Order createOrder(OrderCreateContext ctx) {
       Order saved = orderRepository.save(...);
       outboxService.publishOrderCreatedEvent(saved);  // ← aynı tx
       return saved;
   }
   ```

5. **Consumer tarafında** `@KafkaListener`'a yeni case ekle (aşağıda detay).

6. **Breaking change yok** — event-contracts'a sadece additive değişiklik. Field silme, rename, tip değişikliği yasak.

### 2. `@Transactional(propagation = MANDATORY)` kritik

`publishXxxEvent` metotları **her zaman** bu propagation ile işaretlenmeli. Sebep:

- **Dış transaction zorunlu** — tek başına çağrılamaz
- Ayrı transaction açmaz → outbox kaydı iş verisiyle **aynı atomic unit** içinde commit olur
- Rollback olursa outbox da rollback olur → sahte event gönderilmez

Yanlış: `@Transactional(REQUIRES_NEW)` veya propagation'sız. Yanlış olur, atomicity bozulur.

### 3. Outbox entity

```java
@Entity
@Table(name = "outbox")
@Getter @Setter
@SuperBuilder          // ← @Builder DEĞİL — BaseOutbox da @SuperBuilder kullanıyor
@NoArgsConstructor
public class Outbox extends BaseOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

**`@AllArgsConstructor` koyma** — `@SuperBuilder` + `@NoArgsConstructor` yeterli, gereksiz ctor oluşturur.

## Consumer tarafı

### 1. Kafka listener pattern

```java
@KafkaListener(topics = EventConstants.AGGREGATE_PRODUCT, groupId = "search-service-group")
public void consumeProductEvents(
        String messagePayload,
        @Header(value = "message_type", required = false) String headerEventType) {

    try {
        // Debezium JSONB'yi string olarak yayar, bir kez daha unwrap et:
        String unescapedJson = objectMapper.readValue(messagePayload, String.class);

        switch (headerEventType) {
            case EventConstants.EVENT_PRODUCT_CREATED -> handleProductCreated(unescapedJson);
            case EventConstants.EVENT_PRODUCT_UPDATED -> handleProductUpdated(unescapedJson);
            case EventConstants.EVENT_PRODUCT_DELETED -> handleProductDeleted(unescapedJson);
            case null, default -> log.warn("Bilinmeyen PRODUCT event tipi: {}", headerEventType);
        }
    } catch (Exception e) {
        log.error("PRODUCT event işlenirken hata: {}", e.getMessage(), e);
    }
}

private void handleProductCreated(String json) throws JsonProcessingException {
    ProductCreatedEventPayload payload =
            objectMapper.readValue(json, ProductCreatedEventPayload.class);
    // ... iş mantığı
}
```

### 2. ASLA payload'dan eventType okuma

**Yanlış (geçmişte yaşandı):**
```java
JsonNode rootNode = objectMapper.readTree(json);
String eventType = rootNode.get("eventType").asText();  // ← YANLIŞ
```

Event payload record'ları **sadece iş datası** tutar. Tip bilgisi taşımazlar. Tip `message_type` header'ından okunur.

**Neden bu önemli:** Payload'dan okursan `event-contracts` record'una "eventType" field'ı eklemek zorunda kalırsın, bu record'un anlamını bozar ve breaking change olur.

### 3. Inbox idempotency (önerilir ama şu an search-service kullanmıyor)

Kafka **at-least-once** semantiğindedir — aynı mesaj iki kere gelebilir. Idempotency için `InboxService.isMessageProcessed`:

```java
@Transactional
public void handleProductCreated(String messageId, String eventType, String payload) {
    if (inboxService.isMessageProcessed(messageId, eventType, payload)) {
        return;  // daha önce işlendi
    }
    // ... iş mantığı
}
```

`isMessageProcessed` içinde `REQUIRES_NEW` kullanılır — PK çakışması fırlatırsa ana consumer tx'i rollback olmasın.

## BaseInbox ve BaseOutbox referansı

### BaseOutbox (`common-lib/entity/BaseOutbox.java`)

```java
@MappedSuperclass
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseOutbox {
    @Column(nullable = false, length = 100)
    private String aggregateType;     // → Kafka topic

    @Column(nullable = false, length = 255)
    private String aggregateId;       // → Kafka key

    @Column(nullable = false, length = 255)
    private String messageType;       // → Kafka header "message_type"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String messagePayload;    // → Kafka value

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
}
```

### BaseInbox (`common-lib/entity/BaseInbox.java`)

```java
@MappedSuperclass
@Getter @Setter
@SuperBuilder
@NoArgsConstructor
public abstract class BaseInbox {
    @Id
    @Column(name = "message_id", length = 255, nullable = false, updatable = false)
    private String messageId;

    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private InboxStatus status = InboxStatus.PENDING;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false, nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
```

### Servis-özel entity'lerde

**Outbox:**
```java
@Entity @Table(name = "outbox")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public class Outbox extends BaseOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

**Inbox:**
```java
@Entity @Table(name = "inbox")
@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor
public class Inbox extends BaseInbox {
    // Hiçbir field yok — BaseInbox'tan geliyor
}
```

**Repository tipleri kritik:**
```java
public interface OutboxRepository extends JpaRepository<Outbox, Long> { }
public interface InboxRepository extends JpaRepository<Inbox, String> { }  // ← String, Long değil!
```

`InboxRepository` Long yazarsan: bean oluşur, ama `findById(String)` runtime'da patlar. Bu gerçek bir tuzak, uyanık ol.

## Tuzaklar (tekrar olmasın)

### @Builder vs @SuperBuilder

Parent `@SuperBuilder` ise subclass da `@SuperBuilder` olmalı. `@Builder` koyarsan parent field'ları görünmez, `builder().messageId(...)` çağrısı "cannot find symbol" verir.

### InboxRepository<Inbox, Long>

`BaseInbox.messageId` `@Id String`. Repository generic `JpaRepository<Inbox, String>` olmalı. Long yazarsan bean oluşur, runtime'da sorgular patlamaya başlar.

### Consumer'da `rootNode.get("eventType")` hack'i

Yapma. Header'dan oku: `@Header(value = "message_type")`. Debezium config'de `additional.placement: "message_type:header:message_type"` var (bkz. `debezium-connector-config` skill).

### Payload unescape iki aşamalı

Debezium JSONB'yi outbox'tan okurken string'e escape eder, sonra Kafka value'ya koyar. Consumer'da:
1. `objectMapper.readValue(messagePayload, String.class)` → unescape
2. `objectMapper.readValue(unescapedJson, MyPayload.class)` → gerçek record

Bu iki adımı birleştirip `readValue(messagePayload, MyPayload.class)` yazarsan patlarsın.

### `@Transactional` self-invocation

Aynı sınıf içinde başka metot çağrısı proxy üzerinden geçmez:
```java
public class Service {
    public void outer() {
        this.inner();  // ← proxy atlanır, @Transactional çalışmaz
    }
    @Transactional
    public void inner() { ... }
}
```

Çözüm: `inner()`'ı ayrı bean'e al, veya `AopContext.currentProxy()` kullan. Detay: CONVENTIONS §16.6.

## İlişkili dosyalar

- Kurallar: `CONVENTIONS.md` §7 (outbox), §3.3 (tx sınırları), §16 (tuzaklar)
- Mimari: `ARCHITECTURE.md` §3 (event pipeline)
- Connector config: `debezium-connector-config` skill
- Mevcut örnekler: `backend/product-service/src/main/java/com/ecommerce/productservice/outbox/`, `backend/stock-service/.../outbox/`, `backend/search-service/.../product/consumer/`
