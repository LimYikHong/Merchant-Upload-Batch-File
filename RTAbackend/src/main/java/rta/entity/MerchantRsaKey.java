package rta.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_rsa_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRsaKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    /**
     * INBOUND = public key (merchant encrypts uploads with this) OUTBOUND =
     * private key (merchant decrypts return files with this)
     */
    @Column(name = "key_purpose", nullable = false)
    private String keyPurpose;

    @Column(name = "rsa_public_key", nullable = false, columnDefinition = "TEXT")
    private String rsaPublicKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
