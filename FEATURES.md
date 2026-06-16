# Özellikler (README için kaynak)

Bu dosya geliştirilen özelliklerin canlı bir kaydıdır; proje bitiminde README'ye süzülür.
Mimari: Spring Boot microservice + React/TS SPA + Keycloak + iyzico + Kafka/Debezium (outbox CDC) + Elasticsearch + Redis + MinIO.

---

## Müşteri (Storefront)

- **Ürün arama (faceted):** Elasticsearch ile tam-metin arama + kategori, marka facet'leri, fiyat aralığı,
  puan, stok, sıralama (yeni/fiyat/puan/popüler). Autocomplete (öneri) arama çubuğunda.
- **Tek mağaza sayfası:** `/store/:tenantId` — o mağazaya özel faceted arama + filtreler + "⭐ Öne Çıkanlar" şeridi.
- **Ürün detayı + varyant seçimi:** Beden/numara/renk gibi varyantlar; seçilince fiyat/görsel/SKU güncellenir.
  Varyant seçmeden ana ürün satılamaz (backend guard); stoğu olmayan varyant çarpılı, "Son X adet" uyarısı.
- **Öneri rail'leri (Trendyol-vari):**
  - **Son Gezdiklerin** — giriş yapmış kullanıcının son gezdiği ürünler.
  - **Senin İçin Önerilenler** — son gezilen + favori tohumlarına göre içerik-tabanlı öneri (AI Engine'e hazır seam).
  - **Benzer Ürünler** — ürün detayında aynı kategori/marka, popülerliğe göre.
  - **Çok Satanlar** — satış + görüntülenme popülerliğine göre trend.
  - **Birlikte Sıkça Alınanlar** — co-purchase (aynı siparişte geçen ürünler).
- **Sepette Unuttukların** — ana sayfada sepet hatırlatma banner'ı.
- **Favoriler (wishlist)**, **sepet** (misafir + üye birleştirme), **çoklu adres**.
- **Değerlendirme/yorum** — puan, yorum, görsel, satıcı yanıtı, faydalı oyu.

## Satıcı (Merchant Panel)

- **Ürün yönetimi:** CRUD, görsel yükleme (MinIO), arama + satış durumu filtresi + sıralama
  (çok satan/çok görüntülenen/fiyat), satır içi popülerlik (👁/🛒), "öne çıkar" (yıldız) toggle.
- **Varyant yönetimi:** matris üretici (eksen kombinasyonları), varyant başına SKU/fiyat/stok, varyant stok görünürlüğü.
- **Stok & depo:** çok depolu stok, manuel ekleme/düşürme (idempotent), SKU arama, **ürün-bazlı düşük-stok eşiği** + uyarı rengi.
- **Sipariş yönetimi:** arama (sipariş no/e-posta) + durum filtresi (server-side), kargoya verme/teslim, takip no.
- **İade yönetimi:** teslim sonrası iade talepleri, onay/red (gerçek iyzico refund), arama.
- **Satış analitiği:** ürün/mağaza metrikleri, en çok satan/görüntülenen.
- **Abonelik/komisyon:** plan, kart yönetimi, komisyon oranı.

## Platform Admin

- Mağaza/ürün/kullanıcı/sipariş yönetimi (arama + filtre + sayfalama), kategori CRUD, iade override, genel metrikler.

## Altyapı / Mimari Öne Çıkanlar

- **Event-driven:** transactional outbox + Debezium CDC → Kafka; servisler arası async (inbox idempotency).
- **Popülerlik pipeline:** `view_count` **Redis-buffer + periyodik batch flush** (hot-row contention yok),
  `sale_count` sipariş onayı event'inden; **dirty-flag** ile yalnız değişen ürünler ES'e partial-update'lenir
  → "popular" sıralaması. ES varyantlı üründe stok = varyantların toplamı (aggregate inStock).
- **Kişiselleştirme verisi:** `user_activity` — ürün görüntüleme + arama geçmişi (öneri + gelecek AI beslemesi).
- **Güvenlik:** Keycloak OAuth2/JWT (dual-mode doğrulama), tenant RBAC, API Gateway yönlendirme.

---

## Bekleyen / Gelecek

- AI Engine (FastAPI): gerçek kişisel öneri ranking'i + yorum sentiment analizi (veri toplanıyor, seam hazır).
- Kafka `PRODUCT_VIEWED` event stream (çok yüksek hacim için Redis-buffer'ın sonraki adımı).
- "Son aramalar" görünür dropdown UI (veri + endpoint hazır).
- Kalite fazı: birim/entegrasyon test, load test, pentest (QUALITY-ROADMAP.md).
