# Order SAGA Design — Pay-First Choreography

> **Durum: BACKEND TAMAMLANDI** (2026-06-01)
> Adım 1–10 implementasyonu bitti. Kalan: api-gateway route + frontend (Stage 10.6).
> Bilinen teknik borç: `TECHNICAL-DEBT.md` "Order Service" bölümüne taşındı.
> Test rehberi: `notlar/OrderService_Test_Rehberi.txt`



Bu dosya order-service SAGA mimarisini belgeler. Tüm implementasyonlar buraya dayanır.

---

## SAGA Tipi: Pay-First Choreography

**Pattern:** Önce ödeme al, sonra async fulfill et.  
Amazon, Shopify, Trendyol dahil çoğu e-ticaret sistemi bu modeli kullanır.

**Neden:**  
- Kart verisi hiçbir yerde saklanmaz — isteğin ömrünü geçmez  
- Kullanıcı hemen ödeme sonucunu görür (UX doğru)  
- SAGA choreography gerçek: stok commit + bildirimler tamamen async  
- Race condition doğru yönetilir: ödeme öncesi sync rezervasyon  

---

## Checkout Akışı (HTTP isteği boyunca — sync)

```
POST /api/v1/orders
  Headers: Idempotency-Key: <uuid>
  Body: { shippingAddress, cardInfo, note? }
  (items basket-service'ten alınır, fiyat product-service'ten snapshot alınır)

1. Basket'ten item'ları al        → BasketServiceClient.getMyBasket()
2. Her item için fiyat snapshot   → ProductServiceClient.getSnapshot(productId)
   price, name, imageUrl, currency, salesStatus (ACTIVE mi?)
   Frontend/basket fiyatına güvenilmez — bu güvenlik kuralı.

3. Stok rezervasyonu (sync Feign) → StockServiceClient.reserve(items, orderId)
   stock-service: tüm item'lar için tek @Transactional içinde reserveStockForOrder()
   @Version (optimistic lock) → race condition'da biri 409 alır, double-booking olmaz
   Başarısız → 409 CONFLICT döner, order oluşturulmaz, temiz çıkış.

4. Ödeme (sync Feign)             → PaymentServiceClient.processOrderPayment(orderId, amount, cardInfo)
   payment-service iyzico'ya gider
   Başarısız → StockServiceClient.rollback(items, orderId) [compensation, sync]
              → 402 PAYMENT_REQUIRED döner, order oluşturulmaz, temiz çıkış.

5. Order kaydet + outbox yaz (tek DB transaction, atomic)
   Order status: CONFIRMED
   Outbox: ORDER_CONFIRMED_EVENT

6. 201 Created döner — { orderId, status: CONFIRMED, totalAmount }
   Kullanıcı bitti. Geri kalan her şey async.
```

**Bu noktada kullanıcıya karşı her şey tamamlandı.** Ödeme alındı, rezervasyon yapıldı, sipariş onaylandı.

---

## Async Akış (Kafka — ORDER_CONFIRMED_EVENT üzerinden)

```
[Debezium outbox → Kafka ORDER topic]

ORDER_CONFIRMED_EVENT
  ├── stock-service (Kafka listener)
  │     reservedQuantity → commit (stok artık gerçekten düştü)
  │     Başarısız → STOCK_COMMIT_FAILED_EVENT publish
  │
  ├── mail-service (Kafka listener)
  │     Sipariş onay maili gönder
  │
  └── basket-service temizleme
        order-service ORDER_CONFIRMED işlendikten sonra
        BasketServiceClient.clearBasket() — Feign çağrısı (basit ve güvenilir)
```

---

## SAGA Compensation

Compensation yalnızca async adımda (stock commit) gerekebilir. Sync adımlarda zaten anında rollback var.

```
[Senaryo: Ödeme alındı, stok commit async olarak başarısız oldu]

stock-service
  │  STOCK_COMMIT_FAILED_EVENT → outbox → Kafka STOCK topic
  ▼
order-service (Kafka inbox listener)
  │  PaymentServiceClient.refund(orderId, transactionId)  ← Feign
  │  Order status: REFUNDED
  │  ORDER_REFUNDED_EVENT → outbox → Kafka
  ▼
mail-service
     "Ödemeniz iade edildi" maili

```

> **Neden stock commit başarısız olabilir?** Teorik: DB down, constraint violation. Pratikte son derece nadir.  
> Race condition OLAMAZ çünkü stok checkout'ta sync rezerve edildi, commit sadece rakam günceller.

---

## OrderStatus State Machine

```
          ┌──────────────────────────────────────────┐
          │         HTTP Checkout (sync)              │
          │  reserve → pay → atomic save              │
          └──────────────────┬───────────────────────┘
                             │
                         CONFIRMED ──────────────────► SHIPPED ──► DELIVERED
                             │                            ▲
                             │ async commit               │ merchant action
                             ▼                            │
                    [stock commit OK] ────────────────────┘
                             │
                    [stock commit FAIL]
                             │
                         REFUNDED
                             
         (iptal akışı — sadece CONFIRMED ve SHIPPED öncesi)
                         CANCELLED ← user action (CONFIRMED iken)
                             │
                    PaymentServiceClient.refund()
                             │
                         REFUNDED
```

**Status listesi:** `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `REFUNDED`

SUBMITTED/DRAFT yok — order sadece her şey başarılı olunca oluşturulur.

---

## Entity Tasarımı

### Order (BaseEntity extend)

```
userId          UUID       — @CurrentUser'dan (Keycloak ID). Body'den gelen customerId kullanılmaz.
tenantId        Long       — URL path'ten
status          OrderStatus
totalAmount     BigDecimal — product-service snapshot'tan hesaplanır, frontend değerine güvenilmez
currency        String
shippingAddressJson TEXT   — sipariş anındaki adres snapshot (adres sonradan değişse sipariş etkilenmez)
paymentTransactionId String — iyzico transaction ID (refund için gerekli)
cancellationReason  String — nullable
```

### OrderItem (BaseEntity extend, @Version YOK — sadece snapshot)

```
orderId         Long       — FK orders.id
tenantId        Long       — shard query için
productId       Long       — referans (product silinse dahi item kalır)
sku             String     — snapshot
productName     String     — snapshot
productImageUrl String     — snapshot
unitPrice       BigDecimal — snapshot (product-service'ten alınan fiyat)
quantity        Integer
```

### Outbox (BaseOutbox extend)

```java
@Entity @Table(name = "outbox")
@SuperBuilder @NoArgsConstructor
public class Outbox extends BaseOutbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

### Inbox (BaseInbox extend)

```java
@Entity @Table(name = "inbox")
@SuperBuilder @NoArgsConstructor
public class Inbox extends BaseInbox {
    // Custom alan yok — BaseInbox yeterli
}
```

---

## Outbox / Inbox Map

| Publish eden | Event | Consume eden |
|---|---|---|
| order-service | ORDER_CONFIRMED_EVENT | stock-service, mail-service |
| order-service | ORDER_CANCELLED_EVENT | stock-service (rollback), mail-service |
| order-service | ORDER_SHIPPED_EVENT | mail-service |
| order-service | ORDER_REFUNDED_EVENT | mail-service |
| stock-service | STOCK_COMMIT_FAILED_EVENT | order-service |

---

## Servis Bazında Değişiklikler

### product-service — yeni internal endpoint

```
GET /api/v1/internal/products/{productId}/snapshot
Response: { productId, tenantId, name, mainImageUrl, price, currency, salesStatus }
```
`InternalProductController`'a eklenir. Order-service checkout'ta fiyat bunu kullanır.

---

### stock-service — 2 sync endpoint + 1 Kafka listener

**Sync REST (order-service Feign çağırır):**
```
POST /api/v1/stocks/internal/reserve
Body: { orderId, tenantId, items: [{productId, quantity}] }
→ Tüm item'lar tek @Transactional içinde. Biri yetersiz → hepsi rollback → 409

POST /api/v1/stocks/internal/rollback  
Body: { orderId, tenantId, items: [{productId, quantity}] }
→ Ödeme başarısız olunca order-service çağırır (compensation)
```

**Kafka Listener (async):**
```
ORDER_CONFIRMED_EVENT → commit reservation (availableQuantity zaten düşük, reservedQuantity azaltılır)
ORDER_CANCELLED_EVENT → rollback reservation (user iptal)
```

**Idempotency:** Her listener'da `inboxService.isMessageProcessed(messageId)` — mevcut altyapı.  
**IDOR fix:** Her stok mutasyonunda product.tenantId == path tenantId kontrolü.

---

### payment-service — 2 yeni internal endpoint

```
POST /api/v1/payments/internal/order-payment
Body: { orderId, tenantId, amount, currency, cardInfo }
customerId → @CurrentUser'dan (BODY'DEN ALINMAZ — IDOR fix)
Kart log'da maskelenir (PCI-DSS)
Response: { transactionId, status }

POST /api/v1/payments/internal/refund
Body: { orderId, transactionId }
→ iyzico refund API
```

---

### basket-service — 1 yeni endpoint

```
DELETE /api/v1/baskets/me
→ Tüm sepeti temizle. order-service ORDER_CONFIRMED sonrası Feign ile çağırır.
```

---

### mail-service — yeni listener'lar

```
ORDER_CONFIRMED_EVENT → sipariş onay maili
ORDER_CANCELLED_EVENT → iptal bildirim maili
ORDER_SHIPPED_EVENT   → kargo takip maili
ORDER_REFUNDED_EVENT  → iade bildirim maili
```

---

### api-gateway — route ekle

```yaml
- id: order-service
  uri: http://order-service:8088
  predicates:
    - Path=/api/v1/orders/**
```

---

### Infra — Debezium connector

`order-service-connector.json` → ORDER topic, EventRouter SMT.  
`register_connector.sh`'a ekle.  
order-service Dockerfile + docker-compose + application-prod.yml.

---

## Yeni event-contracts

Mevcut: OrderCreatedEventPayload ✅, OrderConfirmedEventPayload ✅, OrderCancelledEventPayload ✅, OrderShippedEventPayload ✅

**Eklenecekler:**

```java
// order-service publish eder, mail-service consume eder
public record OrderRefundedEventPayload(
    Long orderId,
    UUID userId,
    Long tenantId,
    String reason
) {}

// stock-service publish eder, order-service consume eder
public record StockCommitFailedEventPayload(
    Long orderId,
    String transactionId,  // refund için
    String reason
) {}
```

**EventConstants'a eklenecek:**
```java
EVENT_ORDER_REFUNDED         = "ORDER_REFUNDED_EVENT";
EVENT_STOCK_COMMIT_FAILED    = "STOCK_COMMIT_FAILED_EVENT";
```

---

## Güvenlik Kuralları (değişmez)

1. **Fiyat integrity:** OrderItem.unitPrice her zaman product-service snapshot'tan. Frontend/basket fiyatına güvenilmez.
2. **customerId:** Her zaman `@CurrentUser`'dan. Body'deki `customerId` field'ı kullanılmaz.
3. **Kart verisi:** isteğin HTTP handler'ını geçmez. Log'a yazılmaz. Maskelenir.
4. **Idempotency:** POST /api/v1/orders `@Idempotent` — aynı Idempotency-Key ile iki kez çağrılırsa ikinci istek double-charge yapmaz.
5. **Internal endpoint'ler:** `/internal/**` path'leri authenticated + `hasRole` korumalı.

---

## Güvenlik Kapıları — Stage 10 Öncesi

| # | Açık | Servis | Durum |
|---|---|---|---|
| 1 | IDOR: customerId body'den | payment-service | ❌ açık |
| 2 | IDOR: product tenant kontrolü | stock-service | ❌ açık |
| 3 | Kart bilgisi log'da düz | payment-service | ❌ açık |
| 4 | Rate limiting yok (checkout) | api-gateway | ❌ açık |
| 5 | Frontend lint CI blocker | frontend | ❌ açık |
| 6 | createTenant catch-all yanlış event | user-tenant-service | ❌ açık |

---

## Implementasyon Sırası

```
Adım 1 — order-service temel (şu an)
  Entity + Flyway V1 (orders, order_items, outbox, inbox)
  OrderStatus enum

Adım 2 — Servis hazırlıkları
  product-service: snapshot endpoint
  stock-service: reserve + rollback sync endpoint, IDOR fix
  payment-service: order-payment endpoint, IDOR fix, card masking

Adım 3 — Güvenlik kapıları
  payment-service IDOR + card masking
  stock-service IDOR
  api-gateway rate limiting
  frontend lint

Adım 4 — order-service checkout endpoint
  POST /api/v1/orders (Feign zinciri: snapshot → reserve → pay → save → outbox)
  Basket temizleme (Feign)
  Idempotency

Adım 5 — Async tamamlama (stock-service Kafka listener)
  ORDER_CONFIRMED_EVENT → stock commit
  ORDER_CANCELLED_EVENT → rollback
  STOCK_COMMIT_FAILED_EVENT → order-service compensation

Adım 6 — order-service inbox + compensation
  STOCK_COMMIT_FAILED listener → refund → ORDER_REFUNDED_EVENT

Adım 7 — Okuma endpoint'leri
  GET /api/v1/orders/me (sayfalandırılmış)
  GET /api/v1/orders/me/{orderId}
  GET /api/v1/orders/tenants/{tenantId} (merchant)
  PUT /api/v1/orders/tenants/{tenantId}/{orderId}/status (shipped/delivered)

Adım 8 — İptal akışı
  POST /api/v1/orders/me/{orderId}/cancel
  Feign: stock rollback + payment refund

Adım 9 — Mail bağlantıları
  mail-service ORDER_* listener'ları

Adım 10 — Container
  Dockerfile + application-prod.yml + docker-compose + Debezium connector

Adım 11 — Frontend
  Checkout flow, sipariş geçmişi, sipariş detay, merchant yönetimi
```
