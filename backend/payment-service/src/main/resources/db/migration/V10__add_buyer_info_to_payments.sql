-- Admin transaction listesinde "kim aldı" gösterimi için alıcı bilgisini denormalize et.
-- PRODUCT_ORDER'da checkout BuyerInfo'sundan doldurulur; SUBSCRIPTION'da null (aktör = mağaza).
ALTER TABLE payments ADD COLUMN buyer_email VARCHAR(320);
ALTER TABLE payments ADD COLUMN buyer_name VARCHAR(200);
