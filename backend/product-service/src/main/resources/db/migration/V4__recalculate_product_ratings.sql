-- V3 seed migration inserted reviews but never called recalculateProductRating.
-- This migration backfills rating_average and review_count for all products
-- using APPROVED reviews only, matching the runtime JPQL logic.

UPDATE products p
SET
    review_count  = sub.cnt,
    rating_average = ROUND(sub.avg_rating::NUMERIC, 2)
FROM (
    SELECT product_id,
           COUNT(*)          AS cnt,
           AVG(rating)       AS avg_rating
    FROM product_reviews
    WHERE status = 'APPROVED'
    GROUP BY product_id
) sub
WHERE p.id = sub.product_id;
