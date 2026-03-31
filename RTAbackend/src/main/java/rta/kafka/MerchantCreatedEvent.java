package rta.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MerchantCreatedEvent {

    private String merchantId;
    private String name;           // matches producer's field name
    private String email;          // matches producer's field name
    private String username;       // matches producer's field name
    private String company;        // matches producer's field name (was merchantBank)
    private String contact;        // matches producer's field name (was merchantContactPerson)
    private String phone;          // matches producer's field name (was merchantPhoneNum)
    private String address;        // matches producer's field name (was merchantAddress)
    private String createdBy;
    private String createdAt;      // ISO string
    private String merchantAccNum;
    private String merchantAccName;
    private String transactionCurrency;
    private String settlementCurrency;
    private String rsaPublicKeyPem;     // RSA public key in PEM format from RTA_BANK
}
