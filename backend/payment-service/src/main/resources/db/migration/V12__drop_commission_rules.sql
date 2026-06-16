-- Ölü tablo temizliği: commission_rules hiçbir repository/service/controller tarafından kullanılmıyordu.
-- Komisyon gerçekte subscription_plans.commission_rate / tenant_subscriptions.commission_rate'ten geliyor.
DROP TABLE IF EXISTS commission_rules;
