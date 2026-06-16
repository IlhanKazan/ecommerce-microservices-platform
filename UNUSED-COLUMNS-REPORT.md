# Kullanılmayan / Atıl DB Kolonları Raporu

Tarih: 2026-06-15 · Kapsam: tüm backend servis DB'leri (product, order, payment, stock, user-tenant, mail, basket)

> **Yöntem:** Her entity'nin alanları çıkarıldı; getter/setter çağrıları + request/response DTO'ları
> + event payload'ları + repository query'leri tarandı. **Not:** MapStruct generated kod `target/`'ta
> olduğundan, bir alan *request veya response DTO'sunda* geçiyorsa "kullanılıyor" sayıldı (getter sayısı 0
> olsa bile). Aşağıdaki bulgular bu düzeltmeden sonra kalan gerçek atıl alanlardır.

## Sınıflandırma

- **A — Tamamen ölü:** hiç okunmuyor, anlamlı yazılmıyor, hiçbir DTO/event'te yok. Düşürülebilir veya
  bir özelliğe bağlanabilir.
- **B — Yüzeye çıkıyor ama hiç doldurulmuyor:** response'a maplenmiş ama hiçbir yerde set edilmiyor →
  client'a hep default/null gidiyor. Ya özelliği tamamla ya da response'tan kaldır.
- **C — Yalnızca-yazılan audit:** oluşturulurken yazılıyor, hiçbir yerde geri okunmuyor. Genelde kasıtlı
  (debug/iz). Silmek opsiyonel, düşük öncelik.

---

## A — Tamamen ölü

| Servis | Tablo.kolon | Durum | Not |
|---|---|---|---|
| product | `products.view_count` | Ölü | `incrementViewCount` repo query'si **var ama hiç çağrılmıyor**; DTO/event'te yok. Hep 0. |
| product | `products.sale_count` | Ölü | Hiç okunmuyor/yazılmıyor. ES'te ayrı `saleCount` alanı **sabit 0** maplenmiş → **"popular" sıralaması işlevsiz.** |
| stock | `stock.low_stock_threshold` | Atıl | Builder'da `5` sabitiyle yazılıyor, hiç okunmuyor. UI'daki "az kaldı" rengi frontend'de hardcoded `≤5`. Ürün-bazlı eşik için tasarlanmış, bağlanmamış. |
| payment | `commission_rules` (**tüm tablo + `CommissionRule` entity**) | Ölü | Hiçbir repository/service/controller referansı yok. Komisyon gerçekte `subscription_plans.commission_rate` / `tenant_subscriptions.commission_rate`'ten geliyor. |
| user-tenant | `users.email_verified` | Ölü | Hiç okunmuyor/yazılmıyor, DTO'da yok. Doğrulama Keycloak'ta. |
| user-tenant | `users.language` | ~~Ölü-vari~~ **KORUNDU** | DÜZELTME: frontend profil ekranında dil seçici UI'ı var (`AccountProfile.tsx`) + `UserRequest`'te mevcut. Backend okumuyor ama kullanıcı tercihi (ileride i18n) → düşürülmedi. |
| user-tenant | `addresses.latitude`, `addresses.longitude` | Ölü | `AddressResponse`'ta var ama request'te yok, hiç set edilmiyor → hep null. Geo özelliği yok. |

## B — Yüzeye çıkıyor ama hiç doldurulmuyor (hep default/null)

| Servis | Tablo.kolon | Not |
|---|---|---|
| product | `products.is_featured` | Detay response'unda var; "öne çıkar" işaretleyen endpoint yok, hiç set edilmiyor → hep `false`. "Öne çıkan ürünler" listesi de yok. |
| product | `products.discount_percentage` | Detay/edit response'unda var; güncellemeler `discounted_price` set ediyor, bunu değil → hep 0. (Yüzde tabanlı indirimle çakışan ikinci model.) |
| order | ~~`orders.cancellation_reason`~~ **KULLANIMDA** | DÜZELTME: `Order.cancel(reason)`/`refund(reason)` metotlarında direkt alan ataması ile set ediliyor (grep `setCancellationReason(` direkt atamayı kaçırmıştı) + admin sipariş detayında gösteriliyor. Ölü DEĞİL → korundu. |
| product | `product_reviews.sentiment_label`, `sentiment_score`, `keywords` | Altyapı **hazır** (`InternalReviewController` + `ReviewSentimentUpdateRequest` callback'i var, `ReviewResponse`'ta dönülüyor) ama dolduracak **AI Engine henüz yazılmadı** → şimdilik hep null. (Planlı.) |

## C — Yalnızca-yazılan audit (düşük öncelik, silmek opsiyonel)

| Servis | Tablo.kolon | Not |
|---|---|---|
| payment | `iyzico_transactions.raw_request`, `raw_response`, `payment_card_hash`, `card_last_four`, `transaction_date` | Audit/debug için yazılıyor, hiç geri okunmuyor. `card_last_four` zaten `tenant_cards.last_four` ile çiftleniyor (o kullanılıyor). |
| stock | `stock_movements.*` (özellikle `reference_id`) | Hareket kaydı yazılıyor ama hiçbir geçmiş/history endpoint'i yok → tablo tümüyle yalnızca-yazılan. `reference_id` (orderId/userId) hiç sorgulanmıyor. |
| mail | `mail_log.template_name` | Loglanıyor, geri okunmuyor. (MailLog zaten doğası gereği log tablosu.) |

---

## Öne çıkan iş fırsatları

1. **`view_count` + `sale_count` → Popülerlik & analitik.** İkisi de tam ölü ve "popular" ES sıralaması
   bunlar olmadan çalışmıyor. Bunları canlandırmak, hem storefront'ta "popüler ürünler"/"çok satanlar"
   hem de merchant analitiğinde "en çok gezilen/satılan" için doğrudan değer üretir. (Kullanıcının asıl
   istediği yön.)
2. **`commission_rules`** — ya kategori-bazlı komisyon özelliğine bağlanır ya da tablo düşürülür (şu an
   tam ölü ve yanıltıcı).
3. **`is_featured`** — "öने çıkan ürünler" vitrini + merchant'ta işaretleme toggle'ı ile canlandırılabilir.
4. **`low_stock_threshold`** — ürün-bazlı düşük-stok eşiği + uyarı (frontend hardcoded `≤5` yerine).
5. **Temizlik adayları (düşür):** `users.email_verified`, `users.language`, `addresses.latitude/longitude`,
   `products.discount_percentage` (discounted_price ile çakışıyor), `orders.cancellation_reason` (ya doldur ya kaldır).
