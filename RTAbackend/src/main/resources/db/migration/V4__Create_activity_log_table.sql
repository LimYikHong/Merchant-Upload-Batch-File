CREATE TABLE rta_merchant_activity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(255),
    activity_type VARCHAR(255),
    description TEXT,
    timestamp DATETIME
);
