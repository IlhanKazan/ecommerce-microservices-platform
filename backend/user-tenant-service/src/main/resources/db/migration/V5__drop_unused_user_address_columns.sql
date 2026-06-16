-- Ölü kolon temizliği (hiç okunmuyor):
--  users.email_verified  → doğrulama Keycloak'ta
--  addresses.latitude/longitude → hiç set edilmiyordu (response'ta hep null), geo özelliği yok
-- NOT: users.language KORUNDU — backend okumuyor ama frontend profil dil seçici UI'ı kullanıyor (ileride i18n).
-- Bu tablolar CDC'de değil (yalnız outbox capture edilir) → Debezium etkilenmez.
ALTER TABLE users     DROP COLUMN IF EXISTS email_verified;
ALTER TABLE addresses DROP COLUMN IF EXISTS latitude;
ALTER TABLE addresses DROP COLUMN IF EXISTS longitude;
