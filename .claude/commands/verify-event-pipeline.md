---
description: Event pipeline'ın sağlıklı çalıştığını doğrula. Connector status, Kafka mesajları (header'larıyla), Elasticsearch durumu, Flyway migration log'larını kontrol eder.
---

Event pipeline'ı (Outbox → Debezium → Kafka → Consumer) baştan sona doğrulayacağım. `debezium-ops` ajanını çağır ve şu komutları **sırayla** çalıştırması, çıktıları analiz etmesini iste.

## Doğrulama adımları

### 1. Connector durumu

```bash
# Çalışan tüm connector'lar
curl -s http://localhost:8083/connectors | jq

# Her birinin status'ü
for connector in $(curl -s http://localhost:8083/connectors | jq -r '.[]'); do
    echo "=== $connector ==="
    curl -s http://localhost:8083/connectors/$connector/status | jq '{name, connector: .connector.state, tasks: .tasks[].state}'
done
```

**Beklenen:** Her connector ve task'i `RUNNING` durumunda. `FAILED` veya `PAUSED` varsa neden — config error mı, slot conflict mi, bak.

### 2. Replication slot'ları

```bash
docker exec -it postgres psql -U $POSTGRES_USER \
  -c "SELECT slot_name, plugin, slot_type, database, active, restart_lsn FROM pg_replication_slots;"
```

**Beklenen:** Her servis için bir aktif slot. `active=t` olmalı. `f` ise connector connect olamamış demek.

### 3. Outbox tablolarında REPLICA IDENTITY kontrolü

```bash
for db in product_catalog_db stock_db user_tenant_db payment_db; do
    echo "=== $db ==="
    docker exec -it postgres psql -U $POSTGRES_USER -d $db \
        -c "SELECT relname, relreplident FROM pg_class WHERE relname = 'outbox';"
done
```

**Beklenen:** `relreplident = 'f'` (FULL). `'d'` ise migration eksik, REPLICA IDENTITY FULL set edilmemiş — Debezium update/delete event'leri eksik alır.

### 4. Kafka topic'leri

```bash
docker exec -it kafka kafka-topics --bootstrap-server kafka:9092 --list
```

**Beklenen:** `PRODUCT`, `STOCK`, `TENANT`, `PAYMENT` topic'leri mevcut. Yoksa connector'lardan event hiç akmamış.

### 5. Topic mesajları (header'larıyla)

Her topic için son 3 mesajı header'la oku:

```bash
for topic in PRODUCT STOCK TENANT PAYMENT; do
    echo "=== $topic ==="
    docker exec -it kafka kafka-console-consumer \
        --bootstrap-server kafka:9092 \
        --topic $topic \
        --from-beginning \
        --property print.headers=true \
        --property print.key=true \
        --max-messages 3 \
        --timeout-ms 5000 || echo "(mesaj yok veya timeout)"
done
```

**Beklenen:** Her mesajın header'ında `message_type:<EVENT_NAME>` görünmeli (örn. `message_type:PRODUCT_CREATED_EVENT`). Header eksikse Debezium config'inde `additional.placement` satırı yok demek.

### 6. Elasticsearch durumu

```bash
# Index var mı, kaç döküman
curl -s localhost:9200/products/_count | jq

# İlk 5 ürünün stok ve isim durumu
curl -s 'localhost:9200/products/_search?pretty&size=5' | \
    jq '.hits.hits[] | {_id, name: ._source.name, inStock: ._source.inStock, status: ._source.status}'
```

**Beklenen:** ES'te ürünler var, `inStock` alanı dolu. Yoksa search-service consumer event'leri işlemiyor.

### 7. Search-service consumer log'u

```bash
docker logs search-service 2>&1 | grep -E "Gelen Tip:|ES Yeni Ürün|ES Güncellendi|Bilinmeyen" | tail -20
```

**Beklenen:**
- `Gelen Tip: PRODUCT_CREATED_EVENT` benzeri log'lar
- `ES Yeni Ürün İndekslendi` veya `ES Güncellendi` satırları
- `Bilinmeyen event tipi` warn'u **OLMAMALI** — varsa header gelmemiş demektir

### 8. Flyway migration durumu (her servis)

```bash
for service in product-service stock-service user-tenant-service payment-service; do
    echo "=== $service ==="
    docker logs $service 2>&1 | grep -iE "migrat|flyway" | head -10
done
```

**Beklenen:** Her servis "Successfully applied N migrations" gibi bir satır göstermeli, hata mesajı olmamalı.

### 9. Outbox tablolarında bekleyen mesaj var mı

```bash
for db_pair in "product_catalog_db product-service" "stock_db stock-service" "user_tenant_db user-tenant" "payment_db payment-service"; do
    set -- $db_pair
    db=$1
    label=$2
    echo "=== $label ($db) — outbox ==="
    docker exec -it postgres psql -U $POSTGRES_USER -d $db \
        -c "SELECT COUNT(*) AS total, MAX(created_at) AS latest FROM outbox;"
done
```

**Beklenen:** Outbox tabloları boş veya çok az mesaj olmalı. Çok mesaj birikiyorsa Debezium hala consume etmiyor demek (cleanup scheduler de çalışmıyor olabilir, TODO).

## Çıktı formatı

`debezium-ops` ajanı her adımı çalıştırdıktan sonra **özet rapor** versin:

```
🟢 Connector status — 4/4 RUNNING
🟢 Replication slots — 4/4 aktif
🟢 REPLICA IDENTITY — 4/4 FULL
🟢 Kafka topics — PRODUCT, STOCK, TENANT, PAYMENT mevcut
🟢 Headers — message_type her topic'te görünüyor
🟢 Elasticsearch — 47 ürün, inStock alanı dolu
🟢 Search consumer — son 20 log'ta hata yok, eventler işleniyor
🟢 Flyway — tüm servisler son migration'da
🟡 Outbox — payment_db'de 142 bekleyen mesaj (cleanup scheduler eksik, TODO)

Pipeline sağlıklı.
```

Sorun varsa:

```
🔴 Connector status: stock-service-connector FAILED
   Sebep: replication slot conflict
   Önerilen aksiyon: slot drop + register_connector.sh

Detay komut: ./infrastructure/scripts/register_connector.sh
```

## Hata durumlarında runbook

Sorun çıkarsa hangi runbook ile çözüleceğini söyle:

| Sorun | Runbook |
|---|---|
| Connector FAILED, slot conflict | CLAUDE.md "Outbox schema değişikliği runbook" |
| Header eksik (`Bilinmeyen tip`) | `debezium-connector-config` skill — `additional.placement` config |
| REPLICA IDENTITY 'd' | Yeni Flyway migration: `ALTER TABLE outbox REPLICA IDENTITY FULL;` |
| Outbox şişmesi | `outbox-cleanup-scheduler` (TECHNICAL-DEBT.md) |
| ES boş ama Kafka'da mesaj var | search-service consumer log'una bak, exception var mı |

Bu komutları kullanıcı **çalıştırır**. `debezium-ops` ajanı plan göstermeli, kullanıcı her adımı onaylamalı, sonra yürütmeli.
