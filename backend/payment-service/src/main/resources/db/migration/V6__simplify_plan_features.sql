-- Features içeriğini sadeleştir: sahte limit kuralları kaldırıldı,
-- sadece dürüst platform açıklamaları bırakıldı. Komisyon oranı zaten
-- ayrı kolon olarak kullanılıyor, features sadece görsel kart içeriği.

UPDATE subscription_plans SET features = '["Sınırsız ürün ve kategori", "Temel mağaza sayfası", "iyzico güvenceli ödeme altyapısı", "Gerçek zamanlı stok takibi"]'
WHERE name = 'Başlangıç';

UPDATE subscription_plans SET features = '["Sınırsız ürün ve kategori", "Gelişmiş mağaza sayfası", "iyzico güvenceli ödeme altyapısı", "Gerçek zamanlı stok takibi", "Öncelikli müşteri desteği"]'
WHERE name = 'Büyüme';

UPDATE subscription_plans SET features = '["Sınırsız ürün ve kategori", "Gelişmiş mağaza sayfası", "iyzico güvenceli ödeme altyapısı", "Gerçek zamanlı stok takibi", "Öncelikli müşteri desteği", "Özel hesap yöneticisi"]'
WHERE name = 'İşletme';

UPDATE subscription_plans SET features = '["Sınırsız ürün ve kategori", "Gelişmiş mağaza sayfası", "iyzico güvenceli ödeme altyapısı", "Gerçek zamanlı stok takibi", "7/24 özel destek hattı", "Özel hesap yöneticisi", "SLA garantisi"]'
WHERE name = 'Kurumsal';
