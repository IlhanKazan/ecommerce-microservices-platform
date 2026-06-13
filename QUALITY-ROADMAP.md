# QUALITY-ROADMAP.md

Bu dosya **test, load/stress, ve güvenlik (pentest)** çalışmalarının yol haritası. Amaç: platformun doğruluğunu, performansını ve güvenliğini **kanıtlanabilir metriklerle** göstermek — çıktılar hem mühendislik kalitesi hem de **CV/portföy deliverable'ı**.

**TODO.md'den farkı:** TODO aktif sprint odaklı; bu dosya proje bitişine doğru yapılacak kalite/güvenlik fazının planı. Bir item önceliklenince TODO.md "Aktif" bölümüne taşınır.

Öncelik işaretleri: 🔴 kritik (kanıt değeri en yüksek) → 🟠 yüksek → 🟡 orta → 🟢 nice-to-have.
Durum: `[ ]` yapılmadı · `[~]` devam · `[x]` bitti.

Çalıştırma kuralı: **Tüm load/pentest sadece kendi lokal/dev ortamında** koşulur — platformun sahibi sensin, dış hedef yok.

---

## 0) Sıralama (gerçekçi akış)

Tune etmeden önce doğruluk kanıtlanmalı; güvenlik en son, sistem stabilken.

1. **Coverage altyapısı + test piramidi** → kritik servisler (order/stock/payment) önce.
2. **SAGA integration testleri** (Testcontainers) → akışın doğruluğu kanıtlansın.
3. **Load / stress test + tuning** → davranış doğruyken performansı ölç ve optimize et.
4. **Güvenlik taraması (DAST/SAST/SCA)** → sistem stabilken.
5. **Raporlama** → her fazın çıktısını `docs/quality/` altında topla.

---

## 1) 🔴 Test stratejisi & coverage

> Sıfırdan ajan yazma — `test-writer` ajanı zaten tanımlı (JUnit + Mockito, Testcontainers, WebMvcTest, WireMock). Onu **net hedefle sür:** "şu serviste şu kritik path, JaCoCo line+branch %X."

### Test piramidi (servis başına bu sırayla)

| Katman | Araç | Neyi kanıtlar |
|---|---|---|
| Unit | JUnit + Mockito | Service business logic, edge case'ler |
| Controller slice | `@WebMvcTest` | DTO↔Context mapping, auth/RBAC, validation |
| Feign client | WireMock | Servisler arası senkron çağrı + Resilience4j (circuit breaker, retry, fallback) |
| Integration | Testcontainers (Postgres 16 + Kafka 7.6.0) | **Gerçek event pipeline** — outbox→Kafka→inbox |

### Coverage hedefi (tek %80 dayatma yok — servise göre)

- 🔴 Kritik servisler (order/SAGA, stock, payment): **%80–85 line + branch**
- 🟡 CRUD ağırlıklı (product, category, user-tenant): **%60–70**
- JaCoCo her servisin `pom.xml`'ine eklenir, `mvn verify` HTML rapor üretir → portföye gider.
- Aggregate coverage özeti README'den linklenir.

### En yüksek getirili 5 test grubu (önce bunlar)

- [ ] **Stock reservation / release** — reserve, confirm, release, yetersiz stok.
- [ ] **Idempotency** — aynı idempotency-key iki kez → tek kayıt.
- [ ] **SAGA compensation** — payment fail → stock release; stok yok → order cancel.
- [ ] **Tenant isolation** — tenant A, tenant B'nin kaydını göremez/değiştiremez (hem test hem güvenlik kanıtı).
- [ ] **Payment circuit breaker** — Resilience4j fallback/timeout davranışı (WireMock).

### Görevler

- [ ] JaCoCo plugin + coverage gate (servis bazında threshold).
- [ ] `test-writer` ajanını kritik servislerden başlatarak sür.
- [ ] CI'da test + coverage raporu artifact olarak yayınla.

---

## 2) 🔴 SAGA / sipariş akışı kanıtı

Olay event-driven olduğu için Mockito **yetmez** — Testcontainers integration testiyle kanıtlanır.

- [ ] **Happy path** senaryosu: sepet → reserve stock → payment → confirm → mail.
- [ ] **Compensation path'leri:** payment fail → stock release; stok yetersiz → order cancel.
- [ ] Her adımda doğrula:
  - outbox kaydı yazıldı mı,
  - Kafka'ya event düştü mü (`message_type` header dahil),
  - karşı servis inbox'ında **idempotent** işlendi mi.
- [ ] **Idempotency** ayrı test grubu (yukarıdaki §1 ile ortak).
- [ ] **Tenant izolasyonu** order/payment seviyesinde.

> Detaylı tasarım: `ORDER-SAGA-DESIGN.md`. Bu testler onun "kanıt" katmanı.

---

## 3) 🟠 Load / stress test & tuning

**Araç:** **k6** (JS script, HTML/JSON rapor, Grafana entegrasyonu temiz, CI'a sokulur). Alternatif: Gatling / JMeter. Portföy görseli için k6 + Grafana en şık.

### Yöntem — "ölçmeden tune etme"

1. **Baseline:** k6 ramp-up senaryosu → `p95/p99 latency`, `RPS`, `error rate`.
2. **Darboğazı gözle (Prometheus/Grafana/Zipkin ayağa kalkmalı):**
   - **HikariCP:** `hikaricp_connections_active`, `_pending`, `_timeout` — pending artıyorsa pool küçük.
   - **Tomcat thread:** `tomcat_threads_busy` vs `_max` — busy max'a yapışıyorsa thread/pool dengesi bozuk.
   - **Kafka consumer lag** — pipeline yük altında geride kalıyor mu.
   - **Zipkin trace** — latency'yi yiyen servis/sorgu (N+1, eksik index).
3. **Tune et → tekrar ölç:** HikariCP `maximum-pool-size` (çekirdek×2–4'ten başla, **tüm servislerin pool toplamı Postgres `max_connections`'ı aşmamalı**), Tomcat `max-threads`, Kafka consumer `concurrency`, Feign/Resilience4j timeout. Her değişiklikten sonra **aynı** senaryoyu koş → **before/after tablosu** çıkar (portföyün altın verisi).

### Görevler

- [ ] k6 senaryoları: katalog browse (read-heavy), checkout/SAGA (write-heavy), auth.
- [ ] External'ları (iyzico, Mailhog) mock/sandbox'la — yoksa onları test edersin, kendi servisini değil.
- [ ] Prometheus/Grafana/Zipkin stack'i çalışır hale getir.
- [ ] Before/after tuning tablosu üret (p95, RPS, pending connection, thread busy).

---

## 4) 🔴 Güvenlik / pentest

**Ana araç:** **OWASP ZAP** (DAST). api-gateway üzerinden tüm endpoint'ler.

- [ ] **ZAP Baseline + Full scan** (passive + active).
- [ ] **Auth-aware tarama** — ZAP'a Keycloak JWT ver, yoksa sadece 401 görür.
- [ ] **Mimariye özel manuel testler** (otomatik tarayıcı yakalamaz):
  - [ ] **Tenant isolation / IDOR** — tenant A token'ıyla tenant B'nin order/product ID'sine eriş. **En kritik multi-tenant açığı.**
  - [ ] **JWT** — expired/manipüle token, `none` algorithm, rol yükseltme (platform-admin claim enjeksiyonu).
  - [ ] **BOLA / BFLA** — merchant endpoint'ine normal user, admin endpoint'ine merchant erişebiliyor mu.

### Tamamlayıcı araçlar (kolay portföy puanı)

- [x] **Trivy** — bağımlılık CVE taraması. ✅ 2026-06-14 — Maven+npm `fs --offline-scan`, 0 CRITICAL / 19 HIGH (3 benzersiz: spring-boot-devtools dev-only, axios, react-router — hepsi patch/minor bump). `docs/quality/trivy-cve-scan.md`. (container `image` taraması ayrı: Dockerfile'lar build edilince.)
- [ ] **OWASP Dependency-Check** — Maven bağımlılık CVE'leri.
- [ ] **Semgrep** — SAST (SQL injection, hardcoded secret, kod seviyesi).
- [x] **Gitleaks** — repoda sızmış secret taraması. ✅ 2026-06-14 — 141 commit tarandı, 2 bulgu (ikisi de false-positive, triage edildi), 0 gerçek secret. `.gitleaks.toml` allowlist + `docs/quality/gitleaks-secret-scan.md`.

> Hepsi lokal/dev'de, sadece kendi platformuna karşı.

---

## 5) 🟠 Raporlama / portföy paketi

Tüm çıktılar `docs/quality/` altında toplanır, README'den linklenir.

- [ ] **JaCoCo HTML** — servis başına % + aggregate.
- [ ] **k6 HTML/Grafana** load raporu + **before/after tuning tablosu** (ör. "p95 420ms→110ms, HikariCP pending 0").
- [ ] **ZAP HTML** raporu + bulgu özeti (tespit → fix → retest temiz).
- [ ] **Trivy / Dependency-Check** CVE raporu.
- [ ] **Grafana dashboard ekran görüntüleri** (load altında thread/pool/lag).
- [ ] `QUALITY-REPORT.md` — yukarıdakileri özetleyen üst seviye rapor (CV linki için).

---

## Bağlı dosyalar

- `ORDER-SAGA-DESIGN.md` — SAGA akış tasarımı (test edilecek davranış).
- `TECHNICAL-DEBT.md` — güvenlik borçları (§4 taramasıyla kesişir).
- `ARCHITECTURE.md` §3 — event pipeline (integration testlerin doğrulayacağı akış).
