package rta.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "external_merchant_info")
@Data
public class ExternalMerchantInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false, unique = true)
    private String merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "merchant_bank")
    private String merchantBank;

    @Column(name = "merchant_code")
    private String merchantCode;

    @Column(name = "merchant_phone_num")
    private String merchantPhoneNum;

    @Column(name = "merchant_address")
    private String merchantAddress;

    @Column(name = "merchant_contact_person")
    private String merchantContactPerson;

    @Column(name = "merchant_status")
    private String merchantStatus;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "merchant_acc_num")
    private String merchantAccNum;

    @Column(name = "merchant_acc_name")
    private String merchantAccName;

    @Column(name = "transaction_currency")
    private String transactionCurrency;

    @Column(name = "settlement_currency")
    private String settlementCurrency;
}
