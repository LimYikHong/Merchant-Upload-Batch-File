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

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(name = "rsa_public_key", nullable = false, columnDefinition = "TEXT")
    private String rsaPublicKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
