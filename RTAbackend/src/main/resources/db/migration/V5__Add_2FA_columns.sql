ALTER TABLE rta_user ADD COLUMN two_factor_secret VARCHAR(255);
ALTER TABLE rta_user ADD COLUMN is_two_factor_enabled BOOLEAN DEFAULT FALSE;
