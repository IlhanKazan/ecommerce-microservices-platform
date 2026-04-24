# CLAUDE.md

Bu dosya Claude Code'a her oturumda yüklenir. **Kısa tut, detay için diğer md'lere yönlendir.**

## Proje nedir?

**IlhanKazan E-Commerce Platform** — çoklu mağaza (multi-tenant) e-ticaret platformu. Spring Boot microservice + React/TS SPA + Keycloak auth + iyzico ödeme + Debezium CDC event pipeline.

Bitirme projesi + staj projesi olarak geliştiriliyor; **TÜBİTAK 2209-A** başvurusu yapıldı. Şu an **dev-only** — prod yok, lokal docker-compose dışında bir ortam yok. Bu demek oluyor ki:
- Dev ortamı olsa da production-grade kod yaz — retry, logging, error handling standart olmalı. Dev-only sadece DB/migration reset esnekliği için.
- DB'yi drop+recreate etmek serbest (veri kaybı dert değil)
- Flyway migration'da checksum uyuşmazlığı çözümü: local volume'ü silip sıfırdan başlatmak
- Breaking API değişiklikleri için downstream versiyonlama derdi yok, frontend ile senkron güncellersin

- Backend: 9 modül (7 Spring Boot servisi + 2 paylaşımlı jar). Planlanan ama henüz yazılmamış 3 servis var (order-service/SAGA orchestrator, mail-service, AI Engine FastAPI).
- Frontend: React 19 + Vite + MUI + Zustand + React Query
- Infra: PostgreSQL, Kafka, Debezium, Elasticsearch, Redis, MinIO, Keycloak, Prometheus/Grafana/Zipkin (planlı)

## Hızlı referanslar

- **Mimari, event akışı, servis topolojisi:** `ARCHITECTURE.md`
- **Kod kuralları, paket yapısı, DTO kuralları, tuzaklar:** `CONVENTIONS.md`
- **Yapılacaklar, aktif migration aşamaları, bilinen borç:** `TODO.md`

Yeni bir işe başlamadan önce **en azından `TODO.md`'nin "Aktif" bölümü + `ARCHITECTURE.md` §3 (event pipeline) + ilgili servisin `CONVENTIONS.md` bölümü** okunmalı.

## Stack ve versiyonlar

Versiyon hassas işler (compiler, Debezium, Kafka client) için referans. Eksik bir şey `pom.xml` / `docker-compose.yml`'den teyit edilir.

- **Java** 21 (`java.version` tüm servislerde)
- **Spring Boot** 3.5.9 (parent her servis `pom.xml`'de hardcoded — parent-POM yok)
- **Spring Cloud** 2025.0.0 (Gateway, OpenFeign)
- **PostgreSQL** 16 (`wal_level=logical` compose'da set edilmiş, Debezium için şart)
- **Kafka** Confluent 7.6.0
- **Debezium** 2.7.3.Final (PostgresConnector + EventRouter SMT)
- **Redis** 6.2-alpine
- **Elasticsearch** 9.3.1 (single-node, xpack.security disabled)
- **Keycloak** 26.4.5 (Custom SPI için `keycloak-server-spi` 26.4.5)
- **MinIO** latest
- **MapStruct** 1.6.3 (annotation processor, `pom.xml`'de `annotationProcessorPaths`'a ekli)
- **Lombok** (Spring Boot dependency'den gelir)
- **Redisson** (basket-service distributed lock için; `common-lib` üzerinden de gelebilir — `pom.xml`'den teyit)
- **iyzipay-java** (payment-service iyzico SDK)
- **Flyway** core + PostgreSQL plugin (Spring Boot'un sağladığı sürümler)
- **Testcontainers** (stock-service'te; Postgres 16 + Kafka 7.6.0 container'ları)
- **Resilience4j** (user-tenant-service'te `payment-service` için circuit breaker + retry + time-limiter config'i var; `application.yml`'de `resilience4j` bloğu)

Frontend: Vite 7, React 19, TypeScript 5.9, MUI 7, TanStack Query 5.90, Zustand 5, react-oidc-context 3.3, Axios 1.13, keycloakify 11.

## Komutlar

```bash
# Kökte .env olmalı (.env.example eksik — TODO.md "Config" maddesine bak)
cp .env.example .env

# Tüm altyapıyı + user-tenant-service'i kaldır
docker compose up -d

# Temiz reset — Kafka/Zookeeper volume'lerini siler, Debezium connector'lar yeniden kurulur
./infrastructure/scripts/reset_kafka_debezium.sh

# Debezium connector'larını (yeniden) kaydet — connector JSON'ları düzenledikten sonra
cd infrastructure/scripts && ./register_connector.sh

# Keycloak SPI jar build + infra klasörüne kopyala
./infrastructure/scripts/build-spi.sh

# Tek servisi IDE dışında çalıştır
cd backend/<servis-adı> && ./mvnw spring-boot:run

# Build — her servis KENDİ klasöründen (parent POM yok, -pl flag'i ÇALIŞMAZ)
cd backend/<servis-adı> && ./mvnw clean package              # testlerle
cd backend/<servis-adı> && ./mvnw clean package -DskipTests  # hızlı

# Frontend
cd frontend/e_commerce_frontend
npm install
npm run dev       # http://localhost:5173
npm run lint
npm run build
npm run build-keycloak   # Keycloakify teması
```

> **Build notu:** Kullanıcı terminal'de `mvnw` komutlarında JDK/JAVA_HOME sorunu yaşıyor. Claude Code build çağrısı yapacaksa **her zaman servis klasörüne `cd` etmeli**, root'tan `-pl` ile çalıştırmamalı (parent POM yok, flag işe yaramaz). Ayrıca ajanlara "kullanıcı açık olarak istemeden build çalıştırma" kuralı konmuştur — kullanıcı IDE'den build alıp çıktı paylaşacak.

### Port haritası (dev)

| Servis | Port |
|---|---|
| api-gateway | 9050 |
| user-tenant-service | 8081 |
| payment-service | 8082 |
| product-service | 8084 |
| search-service | 8085 |
| basket-service | 8086 |
| stock-service | 8087 |
| Keycloak | 8080 |
| Postgres | 5432 |
| Redis | 6379 |
| Kafka (host) | 29092 |
| Kafdrop | 9000 |
| Debezium Connect | 8083 |
| Elasticsearch | 9200 |
| MinIO API / Console | 9005 / 9001 |
| Mailhog SMTP / UI | 1025 / 8025 |
| Frontend (vite) | 5173 |

## Temel mimari kurallar (Claude'un her zaman uyacağı)

Bu kurallar kullanıcı tarafından açıkça belirtildi ve kod tabanında tutarlı uygulanıyor. Sorgulamadan uy.

1. **Service katmanı DTO kabul etmez.** Controller'daki Request/Response DTO'ları service'e geçmez. Service sadece şunları alır: entity, primitive, veya `*Command`/`*Context` record'u. Service şunları döner: entity veya `*Info` record'u (`query/` paketinde, `Serializable`).
2. **Controller business logic içermez.** Controller sadece: DTO→Context map'le (Mapper üzerinden), service çağır, dönen entity/Info'yu Response'a map'le.
3. **Servisler arası iletişim:** Senkron için **Feign**, asenkron için **Kafka (outbox pattern üzerinden Debezium CDC)**. Raw Kafka Producer kullanılmaz; DB'ye outbox kaydı atılır, Debezium onu Kafka'ya yayar.
4. **Redis'e `*Info` record'u konur.** Entity veya Response DTO cache'lenmez. `*Info` record'u `Serializable` implement eder. İstisna: `basket-service`'te Redis **primary store**'dur, orada `@RedisHash` entity kullanılır.

Detaylı kurallar için **`CONVENTIONS.md`** oku.

## Çalışma prensipleri (önemli)

Kullanıcı net belirtti, ihlal etme:

- **Yazmadan önce gerçek dosyayı oku.** Bir helper, utility, sabit olduğunu varsayma — `rg`/`grep` ile ara, dosyayı aç oku. Paket yapısı servisten servise hafif değişir (ör. UTS'de bazı service'ler interface+Impl'e uymuyor), genelleme yapma.
- **"İşte tam çözüm" diye 500 satır yazıp yanlış çıkma.** Emin değilsen önce keşif yap, dosyayı oku, ilgili test komutunu çalıştır, sonra yaz.
- **Hallucination yasak.** Görmediğin bir metot, annotation, config key'i varmış gibi yazma. Bilmiyorsan söyle.
- **Küçük adımlar.** Büyük refactor'da commit başına bir konsept. Frontend ile senkron gitmesi gereken backend API değişikliklerinde `TODO.md` "Frontend <-> Backend" bölümünde açık olan talepler var; önce ona bak.
- **Build/test kullanıcı istemeden çalıştırma.** Kullanıcı IDE'den build alıyor, terminal sorunlu. Kodu yaz, kullanıcıya "şimdi build alman iyi olur" öner, sonucunu bekle.

## ASLA dokunma (kullanıcı tarafından kilitli alanlar)

Bu alanlarda değişiklik yapmak için **açık kullanıcı onayı şart**. Kendiliğinden dokunma.

- **Flyway migration'ları** (`src/main/resources/db/migration/V*.sql`) — mevcut `V1`, `V2`, ... dosyaları **asla** düzenlenmez. Schema değişikliği için **yeni** `V(N+1)__aciklama.sql` eklenir. Dev ortamı olsa bile mevcut migration'ı değiştirmek checksum bozar; sıfırdan başlatma dışında düzeltilmez.
- **`.env` dosyası** — kullanıcı manuel yönetir. Ajanlar değiştirmez.
- **Keycloak realm-export.json ve SPI jar** (`infrastructure/keycloak/import/`, `backend/keycloak-spi/`) — auth akışını bozar. Kullanıcı manuel yönetir.
- **common-lib security + shared base'ler** (`GlobalSecurityConfig`, `JwtAuthConverter`, `FeignClientInterceptor`, `TenantSecurityEvaluator`, `CurrentUserArgumentResolver`, `BaseEntity`, `BaseInbox`, `BaseOutbox`, `InboxStatus`) — tüm servislere yayılır, breaking change hepsini çöker.
- **Debezium connector JSON'ları** (`infrastructure/debezium/*.json`) — sadece `debezium-ops` ajanı düzenlemeli. `register_connector.sh` ile push sonrası Kafka Connect aktifleşir.
- **event-contracts** (`backend/event-contracts/`) — event payload record'ları **kontrat**tır. Breaking change yok, yalnızca additive. Field silmek, isim değiştirmek, tip değiştirmek yasak.
- **`application.yml`, `application-dev.yml`, `docker-compose.yml`** — sadece `devops-infra` ajanı düzenlemeli (sonraki turda gelecek). `backend-refactor` ajanı YAML'a dokunursa Java kafasıyla düzenleyip kırabilir.

## Agent ve Skill mimarisi

Token tasarrufu için proje rol bazlı ajanlar ve ihtiyaç-üzerine-yüklenen skill'lerle organize edilmiştir. Claude Code görevine göre otomatik seçim yapar; sen ajan çağırmak istersen `@ajanisim` ile çağırabilirsin.

### Ajanlar (rol bazlı)

`.claude/agents/` altında:

| Ajan | Ne zaman |
|---|---|
| `backend-refactor` | Java/Spring kodu, endpoint, business logic, entity/repo/service/mapper. Ana iş ajanı. |
| `flyway-migration` | Flyway V-dosyası yazımı. Schema değişikliği. |
| `debezium-ops` | Debezium connector, replication slot, Kafka test, event pipeline doğrulama. |
| `git-committer` | Servis bazında commit mesajı üretme, AI slop yorum temizliği, commit atma (push yok). |

**Sonraki turda gelecek:** `devops-infra` (compose/yml/healthcheck), `test-writer` (Testcontainers + JUnit), `frontend-refactor` (React/TS).

### Skills (ihtiyaca göre yüklenen referanslar)

`.claude/skills/` altında:

| Skill | Ne zaman yüklenir |
|---|---|
| `outbox-inbox-pattern` | Event publish/consume, BaseOutbox/BaseInbox, @KafkaListener işleri |
| `debezium-connector-config` | Connector JSON, slot management, event routing |
| `spring-service-conventions` | Controller/service/entity/DTO/Info/Command kuralları |
| `flyway-migration-rules` | V-file yazımı, REPLICA IDENTITY, drop+recreate pattern'leri |

**Sonraki turda gelecek:** `minio-storage-pattern`, `frontend-data-fetching`, `frontend-state-management`, `frontend-component-patterns`.

### Custom slash command'lar

`.claude/commands/` altında:

- `/commit-by-service` — `git-committer` ajanını çağırıp servis bazında commit'leme
- `/continue-from-todo` — TODO.md'den devam et, bir sonraki adımı belirle

**Sonraki turda gelecek:** `/verify-event-pipeline` (doğrulama komutları).

## Doğrulama komutları (çalıştığından emin olmak için)

Bir değişiklik yaptıktan sonra sessizce bitirme; event pipeline ve consumer'lar çalıştığını gözlemleyerek doğrula.

```bash
# Debezium'da çalışan connector'lar
curl -s http://localhost:8083/connectors | jq

# Belli bir connector'ın aktif config'i ve status'u
curl -s http://localhost:8083/connectors/product-service-connector/config | jq
curl -s http://localhost:8083/connectors/product-service-connector/status | jq

# Kafka'dan mesaj oku (header'larıyla — "message_type" header'ını göreceksin)
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic PRODUCT \
  --from-beginning \
  --property print.headers=true \
  --property print.key=true \
  --max-messages 3

# Elasticsearch ürün sayısı
curl -s localhost:9200/products/_count | jq

# Elasticsearch ilk 10 ürün (stok/isim)
curl -s localhost:9200/products/_search?pretty | jq '.hits.hits[] | {_id, name: ._source.name, inStock: ._source.inStock}'

# Flyway migration durumu — servis başlangıç logunda
docker logs product-service 2>&1 | grep -iE 'migrat|flyway' | head

# Kafdrop UI (görsel)     : http://localhost:9000
# Debezium REST           : http://localhost:8083
# Keycloak admin          : http://localhost:8080
# MinIO console           : http://localhost:9001
```

## Outbox schema değişikliği runbook

Bir servisin `outbox` tablosunda tip/kolon değişikliği yapıldığında (ör. UUID→BIGINT id) Debezium'un replication slot'unu da temizlemek gerekir — connector silmek yetmez.

```bash
# 1. Connector'ı sil
curl -X DELETE http://localhost:8083/connectors/<servis>-connector

# 2. Replication slot'u drop et (kritik — eski şemayı hafızada tutar)
docker exec -it postgres psql -U $POSTGRES_USER -d <servis_db> \
  -c "SELECT pg_drop_replication_slot('<servis>_debezium_slot');"

# 3. Servisi restart et → yeni Flyway migration'ları uygulansın
docker restart <servis>

# 4. Migration'ları doğrula
docker logs <servis> 2>&1 | grep -iE 'migrat|flyway' | head -20

# 5. Connector'ları yeniden kaydet (script önce DELETE yapar, sonra POST eder)
cd infrastructure/scripts && ./register_connector.sh

# 6. Status ve topic doğrula
curl -s http://localhost:8083/connectors/<servis>-connector/status | jq
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 --topic <TOPIC> \
  --from-beginning --property print.headers=true --max-messages 3
```

> **Tuzak:** `REPLICA IDENTITY FULL` outbox tablosunu yeni yarattıktan sonra tekrar set edilmeli (DROP+CREATE sıfırlar). Yeni migration'da `ALTER TABLE outbox REPLICA IDENTITY FULL;` mutlaka ekle.

## Dil ve ton

Kullanıcı **Türkçe, casual, direkt** iletişim kuruyor. Kod tabanında Türkçe iş mesajları ve log'lar yaygın — bunu koru. Yeni kod eklerken çevredeki stile bak; hem Türkçe hem İngilizce kabul edilir ama `exception.errorCode`, method/class/variable isimleri her zaman İngilizce.

## Soru sorulacak durumlar

Şu durumlarda dur, sor:
- `CLAUDE.md` / `ARCHITECTURE.md` / `CONVENTIONS.md` ile **çelişen** bir şey yapman isteniyor.
- Değişiklik "ASLA dokunma" listesindeki bir dosyayı etkiliyor.
- Birden fazla servise yayılan breaking change (ör. event payload field değişikliği).
- Yeni bir Maven dependency eklemek gerekiyor.
- İş `TODO.md`'de olmayan ve açıkça kapsamdan büyük.
