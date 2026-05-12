---
name: debezium-connector-config
description: Use when creating, modifying, or troubleshooting Debezium connector JSON files, managing PostgreSQL replication slots, running register_connector.sh, debugging Kafka event routing, handling REPLICA IDENTITY issues, or migrating outbox schema. Covers connector standard config, slot cleanup runbook, and common Debezium pitfalls.
---

# Debezium Outbox Connector Rehberi

Bu skill IlhanKazan platformunda Debezium Outbox Event Router konfigürasyonu ve operasyonu için gereken her şeyi içerir. **ARCHITECTURE.md §3.2** ve **CONVENTIONS.md §16.1**'in genişletilmiş hali.

## Connector standart şablonu

Her servis için bir connector yazılır. Standart şablon:

```json
{
  "name": "<service>-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "tasks.max": "1",

    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "${POSTGRES_USER}",
    "database.password": "${POSTGRES_PASSWORD}",
    "database.dbname": "<service>_db",

    "topic.prefix": "<service>",
    "plugin.name": "pgoutput",
    "slot.name": "<service>_debezium_slot",
    "publication.autocreate.mode": "filtered",
    "table.include.list": "public.outbox",

    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false",

    "transforms": "outbox",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "${routedByValue}",
    "transforms.outbox.route.by.field": "aggregate_type",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.payload": "message_payload",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.table.fields.additional.placement": "message_type:header:message_type"
  }
}
```

### Dinamik değerler (her servis için değişir)

| Placeholder | Örnek (stock) | Örnek (payment) |
|---|---|---|
| `<service>-connector` | `stock-service-connector` | `payment-connector` |
| `<service>_db` | `stock_db` | `payment_db` |
| `<service>` (topic.prefix) | `stock-service` | `payment-service` |
| `<service>_debezium_slot` | `stock_service_debezium_slot` | `payment_service_debezium_slot` |

### Sabit değerler (değişmez, her connector'da aynı)

`transforms.outbox.*` bloğunun tamamı sabit. Özellikle kritik olan satır:

```
"transforms.outbox.table.fields.additional.placement": "message_type:header:message_type"
```

**Bu satır olmadan event akışı sessizce kırılır** — no error, no warning, sadece mesaj gitmez. Debezium default'ta `type` kolonu arar (bizde `message_type`), bulamaz, sessizce atlar.

Format: `DB_KOLON_ADI:header:HEADER_ADI`. DB'deki `message_type` kolonunu Kafka header'ına `message_type` adıyla koyar.

## Mevcut connector listesi

`infrastructure/debezium/` altında:

- `product-connector.json` → `product_catalog_db.public.outbox` → Kafka topic `PRODUCT`
- `stock-connector.json` → `stock_db.public.outbox` → Kafka topic `STOCK`
- `user-tenant-connector.json` → `user_tenant_db.public.outbox` → Kafka topic `TENANT`
- `payment-connector.json` → `payment_db.public.outbox` → Kafka topic `PAYMENT` (TODO 4.1 ile eklendi)

Yeni bir connector eklerken:
1. JSON dosyasını `infrastructure/debezium/` altına koy
2. `infrastructure/scripts/register_connector.sh`'a satır ekle (aşağıda)
3. `register_connector.sh`'ı çalıştır

## register_connector.sh kuralları

Script mevcut connector varsa siler, sonra yeniden push eder (idempotent). Yeni connector eklerken sadece `register_connector()` çağrısı ekle:

```bash
register_connector "new-service-connector.json" "new-service-connector"
```

İlk argüman dosya adı, ikinci argüman Kafka Connect'teki connector adı.

**Script `envsubst` ile `${POSTGRES_USER}` ve `${POSTGRES_PASSWORD}` değişkenlerini expand eder.** Whitelist kullanır:

```bash
envsubst '${POSTGRES_USER} ${POSTGRES_PASSWORD}' < file.json
```

Whitelist olmazsa Debezium'un kendi `${routedByValue}` placeholder'ı da expand edilir ve boşalır — connector broken gelir. **Whitelist'e sadece `POSTGRES_*` ekle, başka env ekleme.**

## Outbox schema değişikliği runbook

Outbox tablosunda tip/kolon değişikliği yapıldığında (örn. UUID→BIGINT id, kolon ekleme) Debezium'un replication slot'u eski şemayı hatırlar. **Connector silmek yetmez**, slot'u da drop etmek gerekir.

```bash
# 1. Connector'ı sil
curl -X DELETE http://localhost:8083/connectors/<service>-connector

# 2. Replication slot'u drop et (kritik)
docker exec -it postgres psql -U $POSTGRES_USER -d <service>_db \
  -c "SELECT pg_drop_replication_slot('<service>_debezium_slot');"

# 3. Servisi restart et → yeni Flyway migration uygulansın
docker restart <service>
# veya IDE'den spring-boot:run

# 4. Migration'ları doğrula
docker logs <service> 2>&1 | grep -iE 'migrat|flyway' | head -20

# 5. Connector'ları yeniden kaydet (script DELETE yapar, sonra POST eder)
cd infrastructure/scripts && ./register_connector.sh

# 6. Status doğrula
curl -s http://localhost:8083/connectors/<service>-connector/status | jq

# 7. Mesaj geldiğinden emin ol (header'larıyla)
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 --topic <TOPIC> \
  --from-beginning --property print.headers=true --max-messages 3
```

### REPLICA IDENTITY FULL — unutma

Outbox tablosu **yeni yaratıldığında** (DROP+CREATE ile migrate edildiğinde) REPLICA IDENTITY'si default'a döner. Debezium logical replication için `FULL` gerekir. Migration'ın sonuna ekle:

```sql
ALTER TABLE outbox REPLICA IDENTITY FULL;
```

Bu olmazsa Debezium UPDATE/DELETE event'lerini eksik alır.

Referans: `product-service/V2__outbox_CDC_config.sql`, `stock-service/V4__outbox_replica_identity_full.sql`.

## Doğrulama komutları

```bash
# Çalışan connector listesi
curl -s http://localhost:8083/connectors | jq

# Belli bir connector'ın aktif config'i
curl -s http://localhost:8083/connectors/<connector-name>/config | jq

# Status (RUNNING mi, FAILED mi, PAUSED mı)
curl -s http://localhost:8083/connectors/<connector-name>/status | jq

# Slot listesi (PostgreSQL)
docker exec -it postgres psql -U $POSTGRES_USER \
  -c "SELECT slot_name, plugin, slot_type, database, active FROM pg_replication_slots;"

# Kafka topic'leri
docker exec -it kafka kafka-topics --bootstrap-server kafka:9092 --list

# Header'lı mesaj oku — "message_type" header'ını görmelisin
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic <TOPIC> \
  --from-beginning \
  --property print.headers=true \
  --property print.key=true \
  --max-messages 3
```

Beklenen header format:
```
Headers:        message_type:PRODUCT_CREATED_EVENT
Key: 42    Value: "{\"productId\":42,\"tenantId\":...}"
```

## Tuzaklar

### 1. `additional.placement` eksikliği

**En sık yapılan ve en zor tespit edilen bug.** Config'de bu satır yoksa event'ler Kafka'ya gider ama header'sız gelir, consumer header okuyamadığı için sessizce atlar, log'da `Bilinmeyen event tipi: null` warn'u çıkar.

**Debug:** Kafka'dan header ile oku, header boşsa config'e bak.

### 2. Connector adı vs dosya adı karışıklığı

JSON'daki `"name"` değeri Kafka Connect'te kullanılır. `register_connector.sh`'taki ikinci argüman da bu olmalı. Dosya adı farklı olabilir:

| Dosya adı | JSON "name" | Kafka Connect'te |
|---|---|---|
| `product-connector.json` | `"product-service-connector"` | `product-service-connector` |
| `payment-connector.json` | `"payment-connector"` | `payment-connector` |

Tutarsızlık varsa DELETE endpoint'i yanlış adı çağırır, eski connector durur.

### 3. `envsubst` whitelist

Whitelist olmadan `${routedByValue}` expand edilir → boş → connector broken. Her zaman:

```bash
envsubst '${POSTGRES_USER} ${POSTGRES_PASSWORD}' < file.json | curl ...
```

### 4. Slot'u unutmak

Migration sonrası slot invalidation en sık yaşanan. Schema değiştirdiysen **slot'u drop et, connector'ı DELETE et, servisi restart et, script çalıştır**. Sıra önemli — slot drop olmazsa connector re-register yetmez.

### 5. REPLICA IDENTITY

Outbox tablosunu drop+recreate eden her migration'a `ALTER TABLE outbox REPLICA IDENTITY FULL;` ekle. Unutursan Debezium 6 ay sonra hata vermeden eksik event yayar.

## İlişkili dosyalar

- Connector JSON'ları: `infrastructure/debezium/*.json`
- Register script: `infrastructure/scripts/register_connector.sh`
- Reset script: `infrastructure/scripts/reset_kafka_debezium.sh`
- Mimari: `ARCHITECTURE.md` §3.2
- Kurallar: `CONVENTIONS.md` §16.1 + §16.5
- İlişkili skill: `outbox-inbox-pattern` (consumer tarafı), `flyway-migration-rules` (schema değişikliği)
