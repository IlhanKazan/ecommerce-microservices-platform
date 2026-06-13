-- Kullanıcı favori ürünleri (wishlist). Kullanıcı keycloakId'sine bağlı, ürün referansı.
CREATE TABLE product_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_user_product_favorite UNIQUE (user_id, product_id)
);

CREATE INDEX idx_product_favorites_user ON product_favorites(user_id);
