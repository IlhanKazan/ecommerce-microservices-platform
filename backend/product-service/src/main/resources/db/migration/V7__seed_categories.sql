-- Trendyol-vari zengin kategori ağacı (mock seed).
-- 2 seviye: ana kategori (level 0, parent_id NULL) + alt kategori (level 1).
-- Idempotent: slug benzersiz olduğu için ON CONFLICT DO NOTHING — tekrar çalışsa da çoğaltmaz,
-- elle eklenmiş mevcut kategorilerle çakışırsa onları bozmaz.

-- ── Ana kategoriler (level 0) ────────────────────────────────────────────────
-- icon kolonu NULL bırakıldı (frontend kategori kartında ismin baş harfini gösteriyor);
-- ileride admin panelinden ikon/görsel atanabilir.
INSERT INTO categories (name, slug, level, full_path, display_order, is_active) VALUES
    ('Elektronik',                'elektronik',             0, 'elektronik',             1,  TRUE),
    ('Moda',                      'moda',                   0, 'moda',                   2,  TRUE),
    ('Ev & Yaşam',                'ev-yasam',               0, 'ev-yasam',               3,  TRUE),
    ('Kozmetik & Kişisel Bakım',  'kozmetik-kisisel-bakim', 0, 'kozmetik-kisisel-bakim', 4,  TRUE),
    ('Anne & Bebek',              'anne-bebek',             0, 'anne-bebek',             5,  TRUE),
    ('Süpermarket',               'supermarket',            0, 'supermarket',            6,  TRUE),
    ('Spor & Outdoor',            'spor-outdoor',           0, 'spor-outdoor',           7,  TRUE),
    ('Kitap, Müzik, Hobi',        'kitap-muzik-hobi',       0, 'kitap-muzik-hobi',       8,  TRUE),
    ('Oto, Bahçe, Yapı Market',   'oto-bahce-yapi-market',  0, 'oto-bahce-yapi-market',  9,  TRUE),
    ('Pet Shop',                  'pet-shop',               0, 'pet-shop',               10, TRUE)
ON CONFLICT (slug) DO NOTHING;

-- ── Alt kategoriler (level 1) ────────────────────────────────────────────────
-- parent_id, ana kategorinin slug'ı üzerinden join ile çözülür.
INSERT INTO categories (name, slug, parent_id, level, full_path, display_order, is_active)
SELECT v.name, v.slug, p.id, 1, p.slug || '/' || v.slug, v.display_order, TRUE
FROM (
    VALUES
        -- Elektronik
        ('Telefon',              'telefon',              'elektronik', 1),
        ('Bilgisayar & Tablet',  'bilgisayar-tablet',    'elektronik', 2),
        ('Televizyon',           'televizyon',           'elektronik', 3),
        ('Beyaz Eşya',           'beyaz-esya',           'elektronik', 4),
        ('Kulaklık',             'kulaklik',             'elektronik', 5),
        ('Oyun & Konsol',        'oyun-konsol',          'elektronik', 6),
        ('Akıllı Saat & Bileklik','akilli-saat',         'elektronik', 7),
        -- Moda
        ('Kadın Giyim',          'kadin-giyim',          'moda', 1),
        ('Erkek Giyim',          'erkek-giyim',          'moda', 2),
        ('Çocuk Giyim',          'cocuk-giyim',          'moda', 3),
        ('Ayakkabı',             'ayakkabi',             'moda', 4),
        ('Çanta',                'canta',                'moda', 5),
        ('Aksesuar & Takı',      'aksesuar-taki',        'moda', 6),
        ('İç Giyim',             'ic-giyim',             'moda', 7),
        -- Ev & Yaşam
        ('Mobilya',              'mobilya',              'ev-yasam', 1),
        ('Mutfak Gereçleri',     'mutfak-gerecleri',     'ev-yasam', 2),
        ('Ev Tekstili',          'ev-tekstili',          'ev-yasam', 3),
        ('Aydınlatma',           'aydinlatma',           'ev-yasam', 4),
        ('Dekorasyon',           'dekorasyon',           'ev-yasam', 5),
        ('Banyo',                'banyo',                'ev-yasam', 6),
        -- Kozmetik & Kişisel Bakım
        ('Makyaj',               'makyaj',               'kozmetik-kisisel-bakim', 1),
        ('Parfüm & Deodorant',   'parfum-deodorant',     'kozmetik-kisisel-bakim', 2),
        ('Cilt Bakımı',          'cilt-bakimi',          'kozmetik-kisisel-bakim', 3),
        ('Saç Bakımı',           'sac-bakimi',           'kozmetik-kisisel-bakim', 4),
        ('Kişisel Bakım',        'kisisel-bakim',        'kozmetik-kisisel-bakim', 5),
        -- Anne & Bebek
        ('Bebek Bezi & Islak Mendil','bebek-bezi',       'anne-bebek', 1),
        ('Bebek Arabası',        'bebek-arabasi',        'anne-bebek', 2),
        ('Bebek Giyim',          'bebek-giyim',          'anne-bebek', 3),
        ('Oyuncak',              'oyuncak',              'anne-bebek', 4),
        ('Mama & Beslenme',      'mama-beslenme',        'anne-bebek', 5),
        -- Süpermarket
        ('Atıştırmalık',         'atistirmalik',         'supermarket', 1),
        ('İçecek',               'icecek',               'supermarket', 2),
        ('Temel Gıda',           'temel-gida',           'supermarket', 3),
        ('Temizlik',             'temizlik',             'supermarket', 4),
        ('Kahvaltılık',          'kahvaltilik',          'supermarket', 5),
        -- Spor & Outdoor
        ('Spor Giyim',           'spor-giyim',           'spor-outdoor', 1),
        ('Fitness & Kondisyon',  'fitness-kondisyon',    'spor-outdoor', 2),
        ('Outdoor & Kamp',       'outdoor-kamp',         'spor-outdoor', 3),
        ('Bisiklet & Scooter',   'bisiklet-scooter',     'spor-outdoor', 4),
        ('Spor Ayakkabı',        'spor-ayakkabi',        'spor-outdoor', 5),
        -- Kitap, Müzik, Hobi
        ('Kitap',                'kitap',                'kitap-muzik-hobi', 1),
        ('Müzik & Film',         'muzik-film',           'kitap-muzik-hobi', 2),
        ('Kırtasiye & Ofis',     'kirtasiye-ofis',       'kitap-muzik-hobi', 3),
        ('Hobi & Sanat',         'hobi-sanat',           'kitap-muzik-hobi', 4),
        ('Müzik Aletleri',       'muzik-aletleri',       'kitap-muzik-hobi', 5),
        -- Oto, Bahçe, Yapı Market
        ('Oto Aksesuar',         'oto-aksesuar',         'oto-bahce-yapi-market', 1),
        ('Bahçe & Çim',          'bahce-cim',            'oto-bahce-yapi-market', 2),
        ('El Aletleri',          'el-aletleri',          'oto-bahce-yapi-market', 3),
        ('Hırdavat',             'hirdavat',             'oto-bahce-yapi-market', 4),
        ('Boya & Badana',        'boya-badana',          'oto-bahce-yapi-market', 5),
        -- Pet Shop
        ('Kedi',                 'kedi',                 'pet-shop', 1),
        ('Köpek',                'kopek',                'pet-shop', 2),
        ('Kuş',                  'kus',                  'pet-shop', 3),
        ('Akvaryum & Balık',     'akvaryum-balik',       'pet-shop', 4),
        ('Pet Aksesuar',         'pet-aksesuar',         'pet-shop', 5)
) AS v(name, slug, parent_slug, display_order)
JOIN categories p ON p.slug = v.parent_slug
ON CONFLICT (slug) DO NOTHING;
