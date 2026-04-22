package rta.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rta.entity.MerchantRsaKey;
import rta.repository.MerchantRsaKeyRepository;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * FileEncryptionService - Generates a random AES-256-GCM key to encrypt file
 * content. - Encrypts the AES key with the merchant's RSA public key. - Returns
 * an EncryptionResult containing encrypted file bytes, encrypted AES key, and
 * IV.
 */
@Service
public class FileEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FileEncryptionService.class);
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;   // 96-bit IV for AES-GCM
    private static final int GCM_TAG_LENGTH = 128;  // 128-bit auth tag

    private final MerchantRsaKeyRepository rsaKeyRepository;

    public FileEncryptionService(MerchantRsaKeyRepository rsaKeyRepository) {
        this.rsaKeyRepository = rsaKeyRepository;
    }

    /**
     * Result holder for encryption output.
     */
    public static class EncryptionResult {

        private final byte[] encryptedFile;
        private final String encryptedAesKey;  // Base64-encoded RSA-encrypted AES key
        private final String iv;               // Base64-encoded IV

        public EncryptionResult(byte[] encryptedFile, String encryptedAesKey, String iv) {
            this.encryptedFile = encryptedFile;
            this.encryptedAesKey = encryptedAesKey;
            this.iv = iv;
        }

        public byte[] getEncryptedFile() {
            return encryptedFile;
        }

        public String getEncryptedAesKey() {
            return encryptedAesKey;
        }

        public String getIv() {
            return iv;
        }
    }

    /**
     * Encrypt a file's raw bytes using AES-256-GCM, then encrypt the AES key
     * with the merchant's RSA public key.
     *
     * @param merchantId the merchant whose RSA public key to use
     * @param plainBytes the raw file content
     * @return EncryptionResult with encrypted file, encrypted AES key, and IV
     */
    public EncryptionResult encryptFile(String merchantId, byte[] plainBytes) throws Exception {
        // 1. Retrieve merchant's INBOUND RSA public key (used to encrypt uploads)
        MerchantRsaKey rsaKeyEntity = rsaKeyRepository.findByMerchantIdAndKeyPurpose(merchantId, "INBOUND")
                .orElseThrow(() -> new RuntimeException(
                "INBOUND RSA public key not found for merchant: " + merchantId));

        PublicKey rsaPublicKey = decodePublicKey(rsaKeyEntity.getRsaPublicKey());

        // 2. Generate random AES-256 key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        SecretKey aesKey = keyGen.generateKey();

        // 3. Generate random IV for GCM
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);

        // 4. Encrypt file content with AES-GCM
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encryptedFileBytes = aesCipher.doFinal(plainBytes);

        // 5. Encrypt AES key with RSA public key (OAEP padding)
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.getEncoded());

        // 6. Base64-encode the encrypted AES key and IV for transport
        String encryptedAesKeyB64 = Base64.getEncoder().encodeToString(encryptedAesKeyBytes);
        String ivB64 = Base64.getEncoder().encodeToString(iv);

        log.info("File encrypted for merchant {} — AES key encrypted with RSA, IV generated", merchantId);

        return new EncryptionResult(encryptedFileBytes, encryptedAesKeyB64, ivB64);
    }

    /**
     * Decode a Base64-encoded X.509 RSA public key string into a PublicKey
     * object.
     */
    private PublicKey decodePublicKey(String base64PublicKey) throws Exception {
        // Strip PEM header/footer if present
        String cleaned = base64PublicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
