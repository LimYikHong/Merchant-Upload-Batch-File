CREATE TABLE return_batch_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT,
    merchant_id VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    return_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    remarks TEXT,
    received_at DATETIME NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES rta_batches(id)
);
