-- Seed: Demo amaçlı yorumlar — ilk 6 ürüne 3'er APPROVED yorum ekle
-- UUID'ler sabit — migration idempotent (aynı migration iki kez çalışsa da veri bütünlüğü bozulmaz)

INSERT INTO product_reviews (product_id, user_id, rating, title, review_text, status, is_verified_purchase, helpful_count)
SELECT p.id,
       'aaaaaaaa-0000-0000-0000-000000000001'::uuid,
       5,
       'Mükemmel ürün!',
       'Gerçekten çok memnun kaldım. Kalitesi beklentimin çok üzerindeydi, kesinlikle tavsiye ederim.',
       'APPROVED',
       true,
       7
FROM products p ORDER BY p.id LIMIT 6;

INSERT INTO product_reviews (product_id, user_id, rating, title, review_text, status, is_verified_purchase, helpful_count)
SELECT p.id,
       'bbbbbbbb-0000-0000-0000-000000000002'::uuid,
       4,
       'Güzel ürün, kargo hızlıydı',
       'Ürün genel olarak kaliteli. Kargo çok hızlı geldi, paketleme de düzgündü. Küçük bir eksik var ama genel olarak memnunum.',
       'APPROVED',
       true,
       3
FROM products p ORDER BY p.id LIMIT 6;

INSERT INTO product_reviews (product_id, user_id, rating, title, review_text, status, is_verified_purchase, helpful_count)
SELECT p.id,
       'cccccccc-0000-0000-0000-000000000003'::uuid,
       3,
       'İdare eder',
       'Fiyatına göre fena değil ama bazı noktalarda kalite düşük. Daha pahalı alternatifler varsa onlara bakmanızı öneririm.',
       'APPROVED',
       false,
       1
FROM products p ORDER BY p.id LIMIT 6;
