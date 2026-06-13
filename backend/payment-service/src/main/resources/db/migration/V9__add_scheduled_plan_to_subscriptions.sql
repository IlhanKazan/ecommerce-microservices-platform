-- Döngü sonunda uygulanacak (deferred downgrade) bekleyen plan referansı.
-- Renewal job billing tarihinde bu plana geçer ve kolonu temizler.
ALTER TABLE tenant_subscriptions ADD COLUMN scheduled_plan_id BIGINT;
