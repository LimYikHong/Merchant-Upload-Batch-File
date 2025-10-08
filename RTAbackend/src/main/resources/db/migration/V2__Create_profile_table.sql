CREATE TABLE profiles (
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
    profile_photo_url VARCHAR(255)
);
