UPDATE tenants
SET status = 'PENDING_PAYMENT'
WHERE status = 'PENDING';

ALTER TABLE tenants DROP CONSTRAINT IF EXISTS chk_tenants_status;

ALTER TABLE tenants ADD CONSTRAINT chk_tenants_status
    CHECK (status IN (
                      'PENDING_PAYMENT',
                      'PAYMENT_FAILED',
                      'ACTIVE',
                      'PASSIVE',
                      'SUSPENDED',
                      'CLOSED'
        ));