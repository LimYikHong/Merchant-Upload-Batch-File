-- 1. bank_summary_reports: drop FK on batch_id, then rename to bank_batch_id
SET @fk_name_reports = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bank_summary_reports' AND COLUMN_NAME = 'batch_id'
    AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql_reports = IF(@fk_name_reports IS NOT NULL,
    CONCAT('ALTER TABLE bank_summary_reports DROP FOREIGN KEY ', @fk_name_reports), 'SELECT 1');
PREPARE stmt_reports FROM @sql_reports;
EXECUTE stmt_reports;
DEALLOCATE PREPARE stmt_reports;

ALTER TABLE bank_summary_reports CHANGE COLUMN batch_id bank_batch_id BIGINT NULL COMMENT 'Batch ID from bank system (NOT a FK)';

-- 2. return_batch_files: drop FK on batch_id, rename table, rename column, add columns
SET @fk_name_return = (SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'return_batch_files' AND COLUMN_NAME = 'batch_id'
    AND REFERENCED_TABLE_NAME IS NOT NULL LIMIT 1);
SET @sql_return = IF(@fk_name_return IS NOT NULL,
    CONCAT('ALTER TABLE return_batch_files DROP FOREIGN KEY ', @fk_name_return), 'SELECT 1');
PREPARE stmt_return FROM @sql_return;
EXECUTE stmt_return;
DEALLOCATE PREPARE stmt_return;

RENAME TABLE return_batch_files TO bank_return_batches;
ALTER TABLE bank_return_batches CHANGE COLUMN batch_id bank_batch_id BIGINT NULL COMMENT 'Batch ID from bank system (NOT a FK)';
ALTER TABLE bank_return_batches ADD COLUMN return_file_path VARCHAR(500) NULL COMMENT 'Local path or storage URI of decrypted CSV';
ALTER TABLE bank_return_batches ADD COLUMN transaction_count INT NULL;

-- 3. Create bank_return_transactions table
CREATE TABLE bank_return_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    return_batch_id BIGINT,
    bank_transaction_id BIGINT COMMENT 'Transaction ID from bank system',
    merchant_id VARCHAR(50),
    merchant_customer VARCHAR(100),
    masked_pan VARCHAR(50),
    amount_cents BIGINT,
    currency VARCHAR(10),
    status VARCHAR(30) COMMENT 'APPROVED / FAILED / DECLINED',
    remark VARCHAR(500),
    authorization_datetime DATETIME,
    FOREIGN KEY (return_batch_id) REFERENCES bank_return_batches(id) ON DELETE CASCADE
);
