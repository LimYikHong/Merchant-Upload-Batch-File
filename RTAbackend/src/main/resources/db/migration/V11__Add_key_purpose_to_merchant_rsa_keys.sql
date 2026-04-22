-- Add key_purpose column and remove unique constraint on merchant_id
-- Each merchant now has 2 rows: INBOUND (public key) and OUTBOUND (private key)
ALTER TABLE merchant_rsa_keys DROP INDEX merchant_id;
ALTER TABLE merchant_rsa_keys ADD COLUMN key_purpose VARCHAR(20) NOT NULL DEFAULT 'INBOUND';
ALTER TABLE merchant_rsa_keys ADD UNIQUE INDEX uq_merchant_purpose (merchant_id, key_purpose);
