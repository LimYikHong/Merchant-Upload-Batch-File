package rta.model;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rta_user")
@Data
public class MerchantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    private String name;

    private String address;

    private String phone;

    private String email;

    private String password;

    private String username;

    private String company;

    private String contact;

    private LocalDateTime joinedOn;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "transaction_currency")
    private String transactionCurrency;

    @Column(name = "settlement_currency")
    private String settlementCurrency;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "two_factor_secret")
    private String twoFactorSecret;

    @Column(name = "is_two_factor_enabled")
    private boolean twoFactorEnabled;
}
