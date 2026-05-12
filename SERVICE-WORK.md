# SERVICE-WORK.md

Bu dosya servis bazında yapılacak iş listesini tutar. Endpoint, business logic, feature, refactor — her şey servisinin altında. Yeni iş bulunduğunda ilgili servisin altına eklenir.

**TODO.md'den farkı:** TODO aktif sprint odaklı ve cross-cutting; bu dosya servis-spesifik backlog. Bir item önceliklenip aktif iş olduğunda TODO'ya taşınır, burada `- [ ] (TODO.md Stage X.Y)` referansı bırakılır.

---

## API Gateway

- [ ] **Port haritası ve public route'lar elden geçirilecek.** Hangi endpoint public, hangi authenticated, açıkça dokümante edilmeli.
- [ ] **Rate limiting** — Redis + Bucket4j veya Resilience4j RateLimiter. Login, password reset, public search gibi endpoint'lerde özellikle.
- [ ] **Guest ve unprotected endpoint'ler tanımı** — `/api/v1/public/**`, `/api/v1/auth/**` vs. authenticated paths arasında net sınır.

---

## Common Lib

- [ ] **Hexagonal SAGA pattern altyapısı** (order-service için temel). State machine, compensation actions.
- [ ] **`TenantContextHolder`** — ThreadLocal-based, `@CurrentUser` mantığında. Servis metotlarında `Long tenantId` parametresini ortadan kaldırır.
- [ ] **Distributed cache: auth/authz** — şu an Redis'te tenant role cache'i var, genişletilecek (yetki matrisi vb.).
- [ ] **MinIO `ImageService` common-lib'e taşı** — UTS ve product-service ortak hale getirilecek (`minio-storage-pattern` skill'inde detay). Üçüncü servis eklendiğinde zaman geldi.

---

## Basket Service

**Genel durum:** Backend endpoint'leri var, frontend entegrasyonu eksik. **Stage 10.6 checkout akışı için basket frontend önkoşul.**

- [ ] **Frontend entegrasyonu — Stage 10.6 öncesinde yapılmalı:**
  - `GET /api/v1/baskets/me` → `useGetCart` hook (zaten var ama UI'a bağlı değil)
  - `POST /api/v1/baskets/items` → `useAddToBasket` + Idempotency-Key
  - `PUT /api/v1/baskets/items/{itemId}` → `useUpdateCartItem`
  - `DELETE /api/v1/baskets/items/{itemId}` → `useRemoveFromCart`
  - `POST /api/v1/baskets/merge` → login sonrası guest cart merge (react-oidc-context onSignIn callback'inde tetiklenir)
  - Sepet sayfasında stok-yetersiz ürünler için uyarı badge'i
  - Boş sepet durumu + "Alışverişe Devam Et" CTA
- [ ] **Stok doğrulaması** (yorumda var, açılacak): `ProductClientAdapter.validateAndGetProduct` `stockQuantity` döndürünce aktifleştir.
- [ ] **Redisson distributed lock** concurrent ekleme/silme için (zaten var, test eksik).
- [ ] **Sepetten siparişe geçişte fiyat snapshot:** `BasketServiceClient.getMyBasket()` order-service'e sadece `productId + quantity` döndürmeli; fiyat order-service tarafından product-service'den taze alınır. Sepet fiyatını sipariş fiyatı yapmak **güvenlik açığı** (kullanıcı cache manipülasyonu yapabilir).

**Stage 9 container testinde gözlemlenen sorunlar:**
- [ ] **Sepet silme çalışmıyor** — `DELETE /api/v1/baskets/items/{itemId}` frontend'den tetiklenemiyor; frontend entegrasyonu (useRemoveFromCart) bağlı değil.
- [ ] **Stok sınırı yok** — kullanıcı stok miktarından fazla ürün sepete atabiliyor; `validateAndGetProduct` stok kontrolü yorumda, açılacak.

---

## Payment Service

**Genel durum:** Sadece abonelik ödemesi + sub-merchant ürün ekleme yazıldı. Sipariş ödemesi ve güvenlik düzeltmeleri eksik.

- [ ] **IDOR kapatma** — body'deki `customerId` yerine `@CurrentUser` kullan. (TECHNICAL-DEBT 🔴'da) — **Stage 10 başlamadan kapanmalı**
- [ ] **Kart bilgisi maskelenmesi** — log'a yazmadan önce mask (TECHNICAL-DEBT 🔴) — **Stage 10 başlamadan kapanmalı**
- [ ] **iyzico'dan dönen hata mesajları UTS'ye Feign ile döndür** — şu an stack trace yutuluyor, UTS sadece "ödeme başarısız" diyor.
- [ ] **Kart token değiştirme endpoint'i**
- [ ] **Yeni kart ekleme endpoint'i**
- [ ] **Sub-merchant mantığı tamamlanması** — sub-merchant create, update, suspend tam akış.
- [ ] **Abonelik bilgileri endpoint'leri** — `GET /subscriptions/me`, `POST /subscriptions/cancel`, upgrade/downgrade.
- [ ] **Outbox cleanup scheduler** (TECHNICAL-DEBT 🟠)
- [ ] **PaymentServiceFallback aktifleştir** (TECHNICAL-DEBT 🟠 — yorum satırında + tip hatası)

### SAGA entegrasyonu için gerekli endpoint (Stage 10 öncesi açılmalı)

Mevcut `processPayment` abonelik ödemesi için. Sipariş ödemesi ayrı bir endpoint istiyor.

- [ ] **`POST /api/v1/payments/internal/order-payment`** — `{ sagaId, orderId, customerId, amount, currency, cardToken }` → iyzico'ya sipariş ödemesi
  - `customerId` BODY'den değil, `@CurrentUser`'dan — IDOR fix buraya bağlı
  - Başarısızsa `PAYMENT_FAILED_EVENT` outbox'a at (sagaId dahil — order-service compensation'ı tetikler)
  - Başarısızsa refund endpoint'i de: `POST /api/v1/payments/internal/refund` — `{ sagaId, transactionId }` → iyzico refund
- [ ] **Fiyat integrity:** order-service `amount`'u product-service'den snapshot alarak gönderir, payment-service frontend'den gelen fiyata güvenmez — kontrol etmez ama loglama yapmalı (amount anomaly detect)

---

## Product Service

**Genel durum:** CRUD oturmuş, edge case'ler ve event akışı eksik.

- [ ] **Silinen ürünü daha önce sipariş verenler.** Soft delete sonrası sipariş geçmişinde ürün referansı korunmalı, ürün detay sayfası read-only mod ile erişilebilir olmalı.
- [ ] **Stokta olmayan ama önceden alınmış ürün ekranı.** Detay görünür, yorumlar görünür, "stokta yok" badge'i, sepete ekle pasif.
- [ ] **`salesStatus` değişikliği için event yayını.** Şu an sadece `PRODUCT_UPDATED_EVENT` yayılıyor — search-service salesStatus'e göre filtre yapamıyor. Yeni event tipi mi (`PRODUCT_SALES_STATUS_CHANGED`) yoksa mevcut UPDATED'a salesStatus eklemek mi (zaten payload'da var, search-service handler'ını düzelt) — kullanıcıya danış.
  - Notion'da: "salesstatus değişince search service haberi olmuyor. event atmamız lazım birçok yerde"
- [ ] **Ürün varyantları** — `parentProductId` field'ı entity'de var ama controller/service tarafı tamamlanmamış (renkli/bedenli ürünler).
- [ ] **Discount/Coupon yönetim endpoint'i** — `discount_percentage`, `discounted_price` kolonları var, business logic eksik.
- [ ] **Review moderasyon akışı** — `PENDING → APPROVED` endpoint yok.
- [ ] **Toplu ürün import (CSV/Excel)** — bulk endpoint, Excel parser.
- [ ] **Internal snapshot endpoint — order-service için:** `GET /api/v1/internal/products/{productId}/snapshot` → `{ productId, tenantId, name, mainImageUrl, price, currency, salesStatus }`. Order-service OrderItem'ı bu snapshot ile oluşturur. Şu an `InternalProductController` var — mevcut `getProductValidation` yeterliyse genişlet, yoksa yeni endpoint.

---

## Search Service

**Genel durum:** Sadece arama yazıldı. Filter/sort/aggregation eksik.

- [ ] **Stokta olmayan ürünleri ES'ten düşür / filtrele.** İki seçenek:
  - (a) Stok bittiğinde ES'ten dökümanı sil → ürün hiç görünmez (mevcut sipariş edenler için sorun)
  - (b) `inStock: false` set et, frontend filter'a ekle (mevcut yaklaşım, tutarlı)
- [ ] **Faceted search** — kategori, marka, fiyat aralığı, attribute (renk/beden) filtreleri.
- [ ] **Autocomplete / suggestion endpoint'i** (ES `completion` field).
- [ ] **Search-service inbox (idempotency)** — TECHNICAL-DEBT 🟡.

---

## Stock Service

**Genel durum:** Manuel stok ekleme, listeleme oturmuş. Sipariş entegrasyonu eksik.

- [ ] **IDOR kapatma — product sahipliği kontrolü** (TECHNICAL-DEBT 🔴).
- [ ] **Inbox aktif değil — duplicate stok düşmesi riski** (TECHNICAL-DEBT 🔴 → çözüm: `BaseInbox` aktifleşmiş ama `InboxService.isMessageProcessed` consumer'da kullanılmıyor).
- [ ] **Stok eşik bazlı uyarı** — `low_stock_threshold` field'ı var, mağaza sahibine bildirim event'i (mail-service ile).

### SAGA entegrasyonu için gerekli endpoint'ler (Stage 10 öncesi açılmalı)

Şu an stock-service'te sadece `manual-add` var. Order-service SAGA orchestrator bu endpoint'lere Feign ile çağrı yapacak.

- [ ] **`POST /api/v1/stocks/internal/reserve`** — `{ tenantId, productId, quantity, sagaId }` → stok rezerve et
  - `@Version` (optimistic lock) — aynı anda iki sipariş yarışırsa biri `OptimisticLockException` → 409 döner
  - Başarısızsa `STOCK_RESERVATION_FAILED` outbox'a at (async SAGA faz 2 için)
  - Hangi depodan çekileceği: `availableQuantity > 0` olan ilk depo (FIFO) — ileride depoya göre strateji eklenebilir
- [ ] **`POST /api/v1/stocks/internal/rollback`** — `{ tenantId, productId, quantity, sagaId }` → rezervasyon iptal (compensation)
- [ ] **`POST /api/v1/stocks/internal/commit`** — `{ tenantId, productId, quantity, sagaId }` → rezervasyonu kalıcıya çevir (sipariş teslim/onay)
- [ ] **`GET /api/v1/stocks/internal/available`** — `{ tenantId, productId }` → toplam kullanılabilir stok (tüm depolar toplamı) — checkout öncesi ön kontrol için
- [ ] Tüm internal endpoint'ler `@PreAuthorize("hasRole('SERVICE')")` veya mTLS ile korunacak (şimdilik servis header kontrolü yeterli; TECHNICAL-DEBT 🔴 `/internal/**`)
- [ ] `STOCK_RESERVED_EVENT` + `STOCK_RESERVATION_FAILED_EVENT` outbox'a ekle — async SAGA faz 2'de kullanılacak
- [ ] **Rollback mekanizması — sipariş iptalinde stok iadesi.** `rollback` endpoint'i ile gelecek; order-service SAGA compensation'ı çağırır.

---

## User Tenant Service

**Genel durum:** En zengin servis, en çok eksik de burada (tracer bullet ile çoğu yer placeholder).

### Mağaza yönetimi (kritik)
- [ ] **Mağaza dondurma (suspend → reactivate).**
- [ ] **Mağaza silme** (soft delete + cascade davranışı: ürünler, stok, kart bilgileri).
- [ ] **`updateTenantCritical` business kuralları.** Önemli alanlar (vergi no, IBAN, sub-merchant data) güncelleme — onay süreci, e-mail doğrulama, belki re-verification.
- [ ] **`updateTenantCritical`'a IBAN ve diğer kritik alan güncellemesi.** Sub-merchant data güncellenecekse iyzico API'sine yansıtılmalı.
- [ ] **`createTenant` hata kodu dönüşü** — şu an generic 500 dönüyor, frontend stepper'da hatalı alanı işaretleyemiyor. errorCode + field-level validation döndürülecek.
- [ ] **`createTenant` idempotency** (TECHNICAL-DEBT 🟡).
- [ ] **`retryTenantPayment` endpoint'i** — yarım kalmış ödeme için retry. Kısmen var, senaryolar (ödeme hâlâ FAILED ise yeni ödeme açılmalı mı, eski transactionId üzerinden retry mı) net değil.
- [ ] **`subMerchantKey` Feign adapter ile alınacak.** Şu an direkt iyzico SDK çağrısı, adapter pattern'i ile sarılmalı.

### Kullanıcı yönetimi
- [ ] **Mağaza çalışan ekleme kuralları.** Davet sistemi, rol atama, çalışan limiti.
- [ ] **Davet tablosu v2** — TODO yorumu var [06.02.2026 14:50].

### Adres
- [ ] **Adres endpoint adres yapısı değişti** — Notion'da "Adres endpoint adresi değişti, düzeltilecek". Frontend ile uyum.
- [ ] **Tenant adresi başlık alanı eksik.** Frontend `tenantAdresCard` form'unda title alanı var ama backend create/update'te field eksik.

### Ödeme entegrasyonu
- [ ] **iyzico SubMerchant pazaryeri açma maili** — Notion'da "Iyzicoya entegrasyon sandbox submerchant pazaryeri açma maili atılacak". Mail-service yazılınca veya direkt SMTP ile.

### Kalite
- [ ] **Hata mesajlarını özelleştir.** Generic "ValidationException" yerine field-level + Türkçe mesaj.
- [ ] **Validation eklenecek** — Notion'da geçiyor, hangi endpoint'lerde belirsiz, taraf taraf bakılacak.
- [ ] **Tüm DB'ler için unique constraint'ler** — örn. `email`, `tax_number`, `sub_merchant_key`. (Şu anda kısmen eksik.)
- [ ] **Feign Adapter yazılacak** — şu an bazı yerlerde Feign client direkt service'ten kullanılıyor. Adapter pattern'i ile sarılmalı (CONVENTIONS §13).
- [ ] **Client `ResponseEntity` dönmemeli** — Notion'da: "Client ResponseEntity dönmemeli, düzelt. domain http ile uğraşmamalı". Feign client interface'i sadece DTO dönmeli.
- [ ] **MinIO veri izolasyonu** (TECHNICAL-DEBT 🟠).
- [ ] **SonarQube sorunları** (TECHNICAL-DEBT 🟢).

---

## Keycloak SPI

- [ ] **Token payload'a tenantId enjekte etme** (TECHNICAL-DEBT 🟡 — performans + temizlik).
- [ ] **Custom user storage SPI test'leri** — şu an hiç test yok (TECHNICAL-DEBT 🟢).

---

## Order Service (henüz yok — TODO.md Stage 10'da detaylı plan)

`order_db` ve `saga_orchestrator_db` init.sql'de tanımlı, kod sıfır.

Detaylı alt-stage planı için → **TODO.md Stage 10** (10.0 event-contracts → 10.1 iskelet → 10.2 Feign → 10.3 SAGA → 10.4 API → 10.5 Outbox/Debezium → 10.6 Frontend)

Özet:
- [ ] **Order entity + state machine** — `DRAFT → SUBMITTED → CONFIRMED → SHIPPED → DELIVERED → CANCELLED`
- [ ] **SAGA orchestrator (senkron Feign faz 1)** — StockServiceClient.reserve → PaymentServiceClient.processOrderPayment → confirm. Başarısız adımda compensation zinciri.
- [ ] **SagaState entity** — sagaId, currentStep, status, compensationData (JSON) — idempotency + debug için
- [ ] **Stock reserve/rollback/commit endpoint'leri** — şu an stock-service'te sadece manual-add var, SAGA için gerekli
- [ ] **PaymentServiceClient.processOrderPayment** — payment-service'e yeni endpoint
- [ ] **Sipariş geçmişi** — kullanıcı + merchant bazında
- [ ] **Sipariş iptal** — stok rollback + ödeme refund
- [ ] **OrderItem snapshot** — productName, unitPrice, imageUrl sipariş anında kopyalanır; ürün silinse dahi geçmiş korunur
- [ ] **ORDER topic Debezium connector** — ORDER_CONFIRMED → mail-service sipariş onay maili

---

## Mail Service

MVP tamamlandı (Stage 8): Kafka consumer, Thymeleaf şablonları, inbox idempotency, Flyway V1, docker-compose.
Şu an sadece TENANT topic dinliyor; aşağıdaki event'ler henüz bağlı değil.

**Mevcut (çalışıyor ama içerik yanlış):**
- [x] `TENANT_ACTIVATED_EVENT` — şu an "mağazanız aktifleştirildi" maili gönderiyor ama bu **yanlış**. ACTIVATED = ödeme alındı, mağaza oluşturuldu, henüz ödeme alamaz. Mail içeriği "mağazanız açıldı, iyzico doğrulama sürecindeyiz" olmalı.
- [x] `TENANT_PAYMENT_FAILED_EVENT` — ödeme başarısız mail ✅

**Eksik — event + handler + şablon eklenecek:**
- [ ] `TENANT_VERIFIED_EVENT` — **"Mağazanız artık aktif!"** maili buraya taşınmalı. VERIFIED = iyzico SubMerchant onaylandı, tenant artık ödeme alabiliyor. `TenantStateService.verifyTenant()` şu an hiç event atmıyor; outbox publish + mail handler + şablon gerekiyor.
- [ ] `TENANT_ACTIVATED_EVENT` şablonu düzelt — içeriği "açılış başarılı, doğrulama sürecindeyiz, 1-3 iş günü" olarak güncelle.
- [ ] `PAYMENT_SUCCESS_EVENT` — ödeme makbuzu. PAYMENT topic'ten consume. `PaymentSuccessEventPayload`'da `recipientEmail` yok — UTS Feign ile resolve edilmeli (bkz. `MailEventConsumer.java` TODO yorumu).
- [ ] `ORDER_CONFIRMED_EVENT` — sipariş onay maili (order-service yazılınca)
- [ ] `ORDER_SHIPPED_EVENT` — kargo takip maili (order-service yazılınca)
- [ ] `LOW_STOCK_EVENT` — mağaza sahibine stok eşik uyarısı (stock-service yazılınca)

---

## AI Engine FastAPI (henüz yok — projenin en son geliştirilecek servisi)

**Stack:** FastAPI + Pydantic + uvicorn + asyncpg (PostgreSQL read-only) + aiokafka (event consume) + Redis (chatbot context) + HuggingFace Transformers + sentence-transformers + scikit-learn + OpenAI/Anthropic API (chatbot LLM).

**Konum:** `backend-ai/` (Java backend'inden ayrı klasörde, ayrı build pipeline). Veya `backend/ai-service/` ama Java toolchain ile karıştırma.

**Auth:** api-gateway'den gelen JWT'yi Keycloak public key ile doğrula, yazma yapmaz, JWT'den `keycloakId` ve `tenantId` claim'leri çıkar.

### Feature listesi

#### 🎯 Yorum & sentiment (kısa vadede en kolay başlangıç)
- [ ] **Yorum özetleme** — ürün için tüm yorumları LLM ile özetler, `Product.aiReviewReport` field'ı zaten hazır.
- [ ] **Sentiment analizi** — ürün yorumları için skor (1-5 yıldız + duygu skoru). BERTurk (`dbmdz/bert-base-turkish-cased`) ile.
- [ ] **Şikayet/iade nedeni kategorize etme** — yorumlardan otomatik etiket çıkarma ("kargo geç", "ürün hatalı", "yanlış beden" vb.).

#### 🛒 Öneri sistemi
- [ ] **Kişisel öneri** — kullanıcının geçmiş siparişleri + sepet + favorilerine göre, collaborative filtering + content-based hybrid.
- [ ] **"Bunu alanlar şunları da aldı"** — item-to-item similarity (frequently bought together).
- [ ] **Personalized homepage** — kullanıcı için ana sayfa ürün sıralaması.
- [ ] **Cold start** — yeni kullanıcı için popüler/trend ürünler.
- [ ] **Recently viewed + öneri** — son gezilen ürünler track edilecek (`PRODUCT_VIEWED_EVENT` gerekecek, yeni event-contracts).

#### 💬 Chatbot / shopping assistant
- [ ] **LLM-powered virtual assistant** — kullanıcı için natural language alışveriş asistanı.
  - `/api/v1/ai/chat` — SSE streaming response endpoint
  - Context: kullanıcının geçmiş siparişleri + favorileri + son aramaları + cart
  - "Bana koşu için ayakkabı öner" → kullanıcı geçmişi + güncel ürün araması + öneri
  - "Geçen ay aldığım kahve makinesi nasıl?" → eski sipariş + ürün detayı
  - "X ürünü hala stokta mı?" → ES query + cevap
- [ ] **Conversation memory** — Redis'te kullanıcı bazlı conversation history (TTL ile, KVKK uyumlu).
- [ ] **Function calling / tool use** — LLM "ürün ara", "siparişi göster", "sepete ekle önerisi" gibi tool'ları çağırsın.

#### 🔍 Search intelligence
- [ ] **Query understanding** — "ucuz kırmızı tişört bedenim L" → ES query DSL (`color: "kırmızı"`, `size: "L"`, `price: <max>`, `category: "tişört"`).
- [ ] **Semantic search** — sentence embeddings (sentence-transformers) ile ürün araması. ES `dense_vector` veya pgvector.
- [ ] **Spell correction + öneri** — "tişşört" → "tişört" düzeltmesi (ES kendi yapabiliyor ama Türkçe için fine-tune değer katar).

#### 📈 Tahmin & analiz (ileri seviye)
- [ ] **Stok talep tahmini** — geçmiş satışlardan trend, low_stock_threshold dinamik. Mağaza sahibine "şu ürünü bitmesin yenile" uyarısı.
- [ ] **Dynamic pricing önerisi** — competitor data (varsa) + stok + sezona göre fiyat önerisi.
- [ ] **Fraud detection** — şüpheli ödeme/sipariş paterni tespiti (yeni hesap + büyük sipariş + farklı IP gibi). payment-service'e webhook.
- [ ] **Anomaly detection** — mağaza bazında sipariş hacmi anomalisi (spike/dip).

#### 🏷️ Mağaza tarafı (B2B asistan)
- [ ] **Kategori auto-tagging** — ürün adı/açıklamadan kategori önerisi (mağaza sahibi listing açarken).
- [ ] **SEO başlık/açıklama önerisi** — `seo_title`, `seo_description`, `seo_keywords` field'ları zaten hazır.
- [ ] **Image recognition** — ürün fotosundan otomatik etiket/kategori önerisi (CLIP veya vision model). Stage 6'daki MinIO upload ile entegre.
- [ ] **Mağaza performans özeti** — haftalık satış raporu, en çok satanlar, az satanlar. (LLM ile doğal dilde özet.)

### Yeni event'ler (event-contracts'e additive)

- `PRODUCT_VIEWED_EVENT` — frontend → product-service → outbox. AI engine consume edip user interest profile günceller.
- `REVIEW_CREATED_EVENT` — product-service review akışı. AI engine sentiment job + product summary update tetikler.
- `RECOMMENDATION_FEEDBACK_EVENT` — kullanıcı önerilen ürüne tıkladı/atladı. AI engine model training için.
- `CART_ITEM_ADDED_EVENT` (opsiyonel) — gerçek zamanlı recommendation refresh.

### Mimari (yüksek seviye)

```
[FastAPI servis — ai-service]
    │
    ├── HTTP (api-gateway arkasında, JWT auth)
    │   ├── /api/v1/ai/chat              (SSE streaming, chatbot)
    │   ├── /api/v1/ai/recommendations   (kişisel öneri)
    │   ├── /api/v1/ai/reviews/{pid}/summary
    │   ├── /api/v1/ai/search/intent     (query → ES DSL)
    │   └── /api/v1/ai/products/{pid}/auto-tag (mağaza için)
    │
    ├── Kafka consumer (event-driven)
    │   ├── PRODUCT topic (created/updated → embedding güncelle)
    │   ├── REVIEW topic (created → sentiment + summary)
    │   ├── ORDER topic (placed → user profile + recommendation refresh)
    │   ├── PRODUCT_VIEW topic (yeni — user interest tracking)
    │   └── PAYMENT topic (success/fail → fraud signal)
    │
    └── Veri katmanı (read-only)
        ├── PostgreSQL read-replica (asyncpg) — kullanıcı geçmişi, ürünler
        ├── Elasticsearch (search + dense_vector embeddings)
        └── Redis (chatbot session, ML cache, recently viewed)
```

### ML stack notları

- **Türkçe NLP:** BERTurk (`dbmdz/bert-base-turkish-cased`) — sentiment, NER, classification
- **Embedding:** `intfloat/multilingual-e5-base` veya OpenAI `text-embedding-3-small` (Türkçe + ürün katalogu için)
- **LLM (chatbot):** Anthropic Claude API veya OpenAI gpt-4o-mini (cost/latency dengesi)
- **Vector DB seçimi:** Elasticsearch `dense_vector` (zaten var, ekstra altyapı yok) **>** pgvector (postgres extension, ek setup) **>** Pinecone (managed, prod ölçek)
- **Recommendation:** scikit-learn cosine similarity + light collaborative filtering, ileride Surprise/Implicit kütüphanelerine geç

### Geliştirilme sırası önerisi (en küçük → en büyük)

1. **Yorum sentiment + summary** (en izole, en kolay) — endpoint döner, kafka eventi consume eder
2. **"Bunu alanlar şunları da aldı"** (collaborative filtering, basit) — historical order data yeter
3. **Recently viewed + cold start öneri** — `PRODUCT_VIEWED_EVENT` yeni eklenir, basit takip
4. **Personalized recommendations** (collaborative + content hybrid)
5. **Search query understanding** (LLM function calling ile)
6. **Chatbot MVP** (sadece ürün aramaya yardımcı, geçmişe bakmaz)
7. **Chatbot full** (geçmiş siparişler, kişisel context)
8. **Mağaza tarafı feature'lar** (auto-tag, SEO, performans özeti)
9. **Tahmin & fraud detection** (en sona — production data lazım)

### Notlar

- Bu servis **kullanıcının özel verisini** taşıyor (alışveriş geçmişi, search history, chatbot conversation). KVKK/GDPR perspektifinden:
  - Conversation history TTL'li (max 30 gün)
  - Recommendation model kullanıcı bazlı opt-out
  - Anonymized aggregation (kullanıcı ID hash'le)
- LLM API çağrıları **maliyet hassas** — caching katmanı zorunlu (Redis), prompt token sayısı kontrol
- Test stratejisi farklı: Java unit test + WireMock pattern'i değil; Python pytest + responses library + factory_boy
- AI ajanı + skill'i **bu servis geliştirmeye başlanınca** kurulacak (`ai-service-fastapi` ajanı, `python-fastapi-conventions` skill, `ml-pipeline-patterns` skill).

---

## Frontend

**Genel durum:** Tracer bullet, çoğu yerde mock data. Asıl iş frontend agent + skill'ler ikinci turda gelecek (CLAUDE.md'de planlı).

### Mağaza & adres
- [ ] **Adres başlık alanı `tenantAdresCard`'a eklenecek** — backend de güncellenecek (UTS section).
- [ ] **Adres endpoint adresi değişti** — frontend service çağrıları güncellenecek.

### Sepet
- [ ] **basket-service endpoint'leri hazır olunca entegrasyon** (`useGetMyBasket`, `useAddToBasket`, `useUpdateBasketItem`, `useRemoveBasketItem`, guest-cart merge hook'u).

### Ödeme akışı
- [ ] **Subscription seçim ekranı** — fetch veya form sorunu var (Notion'da geçiyor, detay belirsiz).
- [ ] **PAYMENT_FAILED ve verified olmayan mağazalara özel banner/mesaj** — durumuna göre dashboard'da uyarı.
- [ ] **`updateTenantCritical` formuna IBAN + sub-merchant alanları**.

### Genel
- [ ] **Idempotency-Key her POST/PUT'a** — Axios interceptor ile UUID üret + header ekle.
- [ ] **Frontend Clean Code pass** — mock'ları kaldırma, dead code, unused import.
- [ ] **Tema ve sayfa modernizasyonu** — Notion'da geçiyor, kapsamı geniş.
- [ ] **Tenant type değişikliği** — type tanımı backend'le eşleşmiyor, sync edilecek.

### Genel teknik
- [ ] **`.env` yorum satırları temizlenecek** (TECHNICAL-DEBT 🟢).
- [ ] **`data/mockOrders.ts`, `data/mockProducts.ts`** prod build'de exclude.

### i18n
- [ ] **Çeviri altyapısı** — şu an her şey Türkçe, EN için planlı (TECHNICAL-DEBT 🟢).

---

## Update protokolü

- Yeni iş geldiğinde: ilgili servisin altına `- [ ]` ile ekle
- İş aktif sprint'e alındığında: TODO.md "Aktif" bölümüne taşı, burada `- [ ] (TODO.md Stage X.Y)` referansı bırak
- İş bittiğinde: TODO.md'de tamamlandı işaretle, burada da `- [x]` yap
- Servis tamamen bittiğinde: servis section'ının başına `## ✅ <ServisAdı>` koy

Notion'dan veya yeni chat'lerden iş eklenirken: önce TECHNICAL-DEBT.md'de cross-cutting mı bak. Cross-cutting değilse buraya gelir.