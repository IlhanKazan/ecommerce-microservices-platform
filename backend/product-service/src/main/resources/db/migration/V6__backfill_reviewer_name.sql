-- Seed verilerindeki null reviewer_name'leri backfill et
UPDATE product_reviews
SET reviewer_name = 'Kullanıcı'
WHERE reviewer_name IS NULL;
