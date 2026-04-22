package rta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import rta.entity.MerchantRsaKey;
import rta.repository.MerchantRsaKeyRepository;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Decrypts return batch files and reports received from RTA Bank.
 *
 * Uses the merchant's OUTBOUND private key (received via Kafka from main
 * system) to decrypt the RSA-wrapped AES key, then decrypts the payload with
 * AES-256-CBC.
 */
@Service
public class ReturnFileDecryptionService {

    private static final Logger log = LoggerFactory.getLogger(ReturnFileDecryptionService.class);

    private final MerchantRsaKeyRepository rsaKeyRepository;

    public ReturnFileDecryptionService(MerchantRsaKeyRepository rsaKeyRepository) {
        this.rsaKeyRepository = rsaKeyRepository;
    }

    /**
     * Decrypt an AES-256-CBC encrypted payload using the merchant's OUTBOUND
     * private key.
     *
     * @param merchantId the merchant whose OUTBOUND private key to use
     * @param encryptedData the encrypted bytes (ciphertext)
     * @param encryptedAesKeyB64 Base64-encoded RSA-encrypted AES key
     * @param ivB64 Base64-encoded IV (16 bytes for CBC)
     * @return the decrypted plaintext bytes
     */
    public byte[] decrypt(String merchantId, byte[] encryptedData, String encryptedAesKeyB64, String ivB64) throws Exception {
        // Load OUTBOUND private key from DB
        MerchantRsaKey outboundKey = rsaKeyRepository.findByMerchantIdAndKeyPurpose(merchantId, "OUTBOUND")
                .orElseThrow(() -> new RuntimeException("OUTBOUND private key not found for merchant: " + merchantId));

        PrivateKey privateKey = decodePrivateKey(outboundKey.getRsaPublicKey());

        // 1. Decrypt AES key with RSA private key
        byte[] encryptedAesKeyBytes = Base64.getDecoder().decode(encryptedAesKeyB64);
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes);

        // 2. Decrypt data with AES-256-CBC
        byte[] ivBytes = Base64.getDecoder().decode(ivB64);
        SecretKeySpec aesKey = new SecretKeySpec(aesKeyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, ivSpec);
        byte[] decryptedData = aesCipher.doFinal(encryptedData);

        log.info("Successfully decrypted {} bytes → {} bytes for merchant {}", encryptedData.length, decryptedData.length, merchantId);
        return decryptedData;
    }

    /**
     * Decode a PEM/Base64-encoded PKCS8 RSA private key.
     */
    private PrivateKey decodePrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
}
