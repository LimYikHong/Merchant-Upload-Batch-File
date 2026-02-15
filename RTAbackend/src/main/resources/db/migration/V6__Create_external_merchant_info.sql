CREATE TABLE external_merchant_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL UNIQUE,
    merchant_name VARCHAR(255),
    merchant_bank VARCHAR(255),
    merchant_code VARCHAR(255),
    merchant_phone_num VARCHAR(255),
    merchant_address VARCHAR(255),
    merchant_contact_person VARCHAR(255),
    merchant_status VARCHAR(255),
    created_by VARCHAR(255),
    created_at DATETIME,
    merchant_acc_num VARCHAR(255),
    merchant_acc_name VARCHAR(255),
    transaction_currency VARCHAR(50),
    settlement_currency VARCHAR(50)
);