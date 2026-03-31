CREATE TABLE bank_summary_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT,
    merchant_id VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    bank_status VARCHAR(50) NOT NULL,
    total_transactions INT,
    successful_transactions INT,
    failed_transactions INT,
    total_amount DECIMAL(19, 2),
    currency VARCHAR(10),
    bank_reference VARCHAR(255),
    remarks TEXT,
    processed_at DATETIME,
    received_at DATETIME NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES rta_batches(id) ON DELETE SET NULL
);
