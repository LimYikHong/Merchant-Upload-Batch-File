CREATE TABLE rta_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255),
    merchant_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE TABLE rta_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    merchant_id VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    remarks VARCHAR(255),
    created_at DATETIME NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES rta_batches(id)
);

CREATE TABLE rta_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    username VARCHAR(255),
    company VARCHAR(255),
    contact VARCHAR(255),
    joined_on DATETIME,
    profile_photo_url VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(100),
    last_modified_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(100),
    deleted_at DATETIME
);
