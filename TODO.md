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

### Sprint 1: Borç temizleme

#### S1-1: Bekleyen commit'ler
- [ ] Stage 9 + Stage 10 değişikliklerini `/commit-by-service` ile servis bazında commit et

#### S1-2: Frontend lint fix (CI blocker)
- [ ] `no-explicit-any` → proper types (15+ satır, 14 dosya — detay: `TECHNICAL-DEBT.md`)
- [ ] `no-unused-vars` → kullanılmayan import'ları sil (7 satır)
- [ ] `NotificationProvider` — context export'u ayrı `NotificationContext.ts` dosyasına taşı
- [ ] `exhaustive-deps` uyarıları düzelt (5 satır)
- [ ] `npm run lint` → 0 error, 0 warning ✓

#### S1-3: mail-service rebuild + verify
- [ ] `docker compose up -d --build mail-service`
- [ ] Sipariş ver → Mailhog UI'da (http://localhost:8025) ORDER_CONFIRMED maili gör
- [ ] TENANT_ACTIVATED şablon içeriği kontrol — yanlışsa düzelt

#### S1-4: activateTenant bug fix
- [ ] `TenantLifecycleService.createTenant` — `activateTenant` çağrısını try bloğu **dışına** taşı
- [ ] Try bloğu yalnızca `processPayment` Feign çağrısını sarsın
- [ ] Test: createTenant → TENANT_ACTIVATED maili gelsin (PAYMENT_FAILED değil)
- [ ] Detay: `TECHNICAL-DEBT.md` "createTenant catch-all" maddesi

#### S1-5: Outbox cleanup scheduler
- [ ] payment-service: `@Scheduled(cron = "0 0 3 * * *")` + `deleteByCreatedAtBefore` ekle
- [ ] basket-service: outbox tablosu var mı kontrol et — varsa aynı pattern
- [ ] Pattern referans: `product-service/OutboxCleanupScheduler` veya `OutboxRepository.deleteByCreatedAtBefore`

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

## 📚 Dokümantasyon & Portfolio

### OpenAPI / Swagger UI
- [ ] Her Spring Boot servisine `springdoc-openapi-starter-webmvc-ui` ekle (`pom.xml`)
- [ ] Her servis `application-dev.yml`'e: `springdoc.api-docs.path=/v3/api-docs`, `springdoc.swagger-ui.path=/swagger-ui.html`
- [ ] Controller'lara `@Tag`, `@Operation`, `@ApiResponse` annotation'ları ekle
- [ ] Checkout + order endpoint'leri öncelikli (portfolio için en etkileyici)
- [ ] Erişim: `http://localhost:808x/swagger-ui/index.html` servis bazında

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
