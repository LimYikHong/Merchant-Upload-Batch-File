-- Add account fields to rta_user
ALTER TABLE rta_user ADD COLUMN account_number VARCHAR(255) NULL;
ALTER TABLE rta_user ADD COLUMN account_name VARCHAR(255) NULL;
ALTER TABLE rta_user ADD COLUMN transaction_currency VARCHAR(50) NULL;
ALTER TABLE rta_user ADD COLUMN settlement_currency VARCHAR(50) NULL;
