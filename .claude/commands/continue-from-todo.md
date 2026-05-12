---
description: TODO.md'deki "Aktif" veya "Sıradaki" bölümünden devam et. Oturumun başında veya /clear sonrası kullan.
---

TODO.md'deki aktif işe devam edeceğim. Şu adımları uygula:

1. **TODO.md'yi oku** — `@TODO.md` dosyasını aç
2. **Aktif ve sıradaki bölümlere bak:**
   - `## ⏭️ Sıradaki` — bu sprint'in kalanları
   - `## 🧩 Frontend ↔ Backend arası açık talepler` — FB-1, FB-2 vb. açık olanlar
   - Aktif Stage ne, son bitirilen item ne
3. **En öncelikli bir sonraki adımı belirle** — genelde:
   - "Verify (runtime)" adımları → kullanıcı test etmesi gereken şeyler
   - `- [ ]` işaretli ilk item
   - Bağımlılığı olmayan, tek başına tamamlanabilecek iş
4. **Kısa özet sun:**
   ```
   📍 Şu anda: Stage 4.1 payment-service migration
   ✅ Kod değişiklikleri yapıldı
   ⏳ Kalan: runtime verify (kullanıcı manuel)

   📍 Sonraki: Stage 4.2 runtime verify, sonra Stage 5

   Hangi adıma odaklanalım?
   - [A] Stage 4.1 runtime verify (ben doğrulama komutlarını vereyim)
   - [B] Stage 4.2 runtime verify (aynı)
   - [C] FB-1: product-service'e image upload endpoint ekle
   - [D] Başka bir iş düşünüyorsun
   ```

5. **Kullanıcı seçim yapsın.** Seçime göre:
   - Runtime verify ise doğrulama komutlarını ver, output'u bekle, analiz et
   - Kod değişikliği ise ilgili ajana/skill'e göre hazırlık yap, plan çıkar, onay al, uygula
   - "D" derse kullanıcıdan detay iste

6. **İş bitince TODO.md'yi güncelle** — item'ı `- [x]` işaretle veya `✅ Tamamlanmış` bölümüne taşı.

**Kurallar:**
- İşi tek başına seçme, kullanıcıya sor
- TODO.md dışında iş ekleme; kullanıcı açıkça isterse ekle
- Birden fazla bağımsız iş varsa (farklı scope'larda) kullanıcıya sor "önce hangisi?"
- Kod yazmadan önce ilgili dosyaları **oku** (ajanın system prompt'u zaten bunu zorunlu kılıyor ama hatırlat)

Başlamadan önce TODO.md'nin tamamını okumak şart. Sadece "Aktif" bölümüne bakıp eksik context'le karar verme.
