package rta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import rta.entity.ConsumerRsaKeyPair;
import rta.repository.ConsumerRsaKeyPairRepository;

import jakarta.annotation.PostConstruct;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Generates and persists an RSA key pair for this consumer system in the DB. On
 * startup, loads existing key pair from consumer_rsa_key_pair table. If none
 * exists, generates a new one and saves it. The public key is served via GET
 * /api/internal/public-key. The private key is used to decrypt incoming
 * encrypted data.
 */
@Service
public class InternalKeyPairService {

    private static final Logger log = LoggerFactory.getLogger(InternalKeyPairService.class);
    private static final int RSA_KEY_SIZE = 2048;

    private final ConsumerRsaKeyPairRepository keyPairRepository;
    private KeyPair keyPair;

    public InternalKeyPairService(ConsumerRsaKeyPairRepository keyPairRepository) {
        this.keyPairRepository = keyPairRepository;
    }

    @PostConstruct
    public void init() {
        Optional<ConsumerRsaKeyPair> existing = keyPairRepository.findTopByOrderByCreatedAtDesc();

        if (existing.isPresent()) {
            // Load from DB
            ConsumerRsaKeyPair stored = existing.get();
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");

                byte[] pubBytes = Base64.getDecoder().decode(stored.getPublicKey());
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubBytes));

                byte[] privBytes = Base64.getDecoder().decode(stored.getPrivateKey());
                PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privBytes));

                this.keyPair = new KeyPair(publicKey, privateKey);
                log.info("Loaded consumer RSA key pair from DB (id={})", stored.getId());
            } catch (Exception e) {
                log.error("Failed to load RSA key pair from DB, generating new one", e);
                generateAndSave();
            }
        } else {
            // Generate new and save to DB
            generateAndSave();
        }
    }

    private void generateAndSave() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(RSA_KEY_SIZE, new SecureRandom());
            this.keyPair = generator.generateKeyPair();

            ConsumerRsaKeyPair entity = new ConsumerRsaKeyPair();
            entity.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            entity.setPrivateKey(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            entity.setCreatedAt(LocalDateTime.now());
            keyPairRepository.save(entity);

            log.info("Generated and saved new consumer RSA key pair ({}-bit) to DB", RSA_KEY_SIZE);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Returns the public key in PEM format (Base64‑encoded X.509).
     */
    public String getPublicKeyPem() {
        byte[] encoded = keyPair.getPublic().getEncoded();
        String base64 = Base64.getEncoder().encodeToString(encoded);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");
        // Wrap at 64 characters per line
        for (int i = 0; i < base64.length(); i += 64) {
            pem.append(base64, i, Math.min(i + 64, base64.length()));
            pem.append('\n');
        }
        pem.append("-----END PUBLIC KEY-----");
        return pem.toString();
    }

    /**
     * Returns the raw PrivateKey object for decryption operations.
     */
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    /**
     * Returns the raw PublicKey object.
     */
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}
