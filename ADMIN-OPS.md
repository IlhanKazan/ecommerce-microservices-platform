# Admin Operasyon Endpoint'leri

Dev ortamında veri tutarsızlığını elle düzeltmek için kullanılan endpoint'ler.
**Tüm endpoint'ler Gateway üzerinden çağrılmaz — doğrudan servis portuna gidilir.**

---

## 1. Ürün Reindex (product-service → Elasticsearch)

**Ne zaman kullanılır:**
- `product_reviews` tablosuna V4 gibi bir migration ile veri girildiğinde (rating_average, review_count güncellendi ama ES'e yansımadı)
- Elasticsearch index'i silip yeniden oluşturulduğunda
- Elasticsearch'te eksik / stale ürün verisi olduğunda

**Ne yapar:** Tüm `ACTIVE` ürünler için `PRODUCT_UPDATED` outbox event'i yazar → Debezium Kafka'ya taşır → search-service consume eder → ES güncellenir.

```bash
curl -X POST http://localhost:8084/api/v1/public/admin/reindex
# Yanıt: "X ürün için reindex başlatıldı."
```

---

## 2. Stok Resync (stock-service → Elasticsearch)

**Ne zaman kullanılır:**
- Elasticsearch'teki `inStock` alanı gerçek stokla uyumsuz olduğunda
- Kafka/Debezium pipeline kesintisi sonrası stok event'leri kaçırıldığında

**Ne yapar:** Pozitif stoklu tüm ürünler için `STOCK_STATUS_CHANGED` outbox event'i yazar → Debezium → Kafka → search-service → ES `inStock: true` set edilir. (Stoksuz ürünler için event yazılmaz — onları ES'te elle `false` yapmak gerekirse index silip reindex tercih et.)

```bash
curl -X POST http://localhost:8087/api/v1/public/admin/stocks/resync
# Yanıt: "X ürün için stok resync tamamlandı."
```

---

## Tipik Senaryo: Migration sonrası tam senkron

```bash
# 1. product-service yeniden başladıktan sonra (Flyway V4 uygulandı)
curl -X POST http://localhost:8084/api/v1/public/admin/reindex

# 2. Birkaç saniye bekle (Debezium → Kafka → search-service işlesin)
sleep 10

# 3. ES'teki ürün sayısını kontrol et
curl -s http://localhost:9200/products/_count | jq

# 4. Birkaç ürünün rating'ini doğrula
curl -s "http://localhost:9200/products/_search?pretty&size=5" | \
  jq '.hits.hits[] | {id: ._id, name: ._source.name, rating: ._source.ratingAverage, reviews: ._source.reviewCount}'

# 5. Stok verisi de stale idiyse
curl -X POST http://localhost:8087/api/v1/public/admin/stocks/resync
```

---

## Notlar

- Her iki endpoint de outbox üzerinden çalışır — işlem anında değil, Debezium event döngüsünde tamamlanır (genellikle <5 sn).
- Gateway'e bağlı değil; port doğrudan servis portudur (`8084` / `8087`).
- Keycloak token gerektirmez (`/api/v1/public/admin/**` path'i public olarak açık — sadece dev ortamı için).
