-- iyzico Refund API per-item paymentTransactionId gerektirir (Cancel paymentId ile, Refund paymentTransactionId ile).
-- Sipariş tek basket-item olarak ödendiği için tek transaction id; iade tutarı bunun üzerinden refund edilir.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_transaction_id VARCHAR(255);
