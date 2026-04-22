CREATE TABLE consumer_rsa_key_pair (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_key TEXT NOT NULL,
    private_key TEXT NOT NULL,
    created_at DATETIME NOT NULL
);
