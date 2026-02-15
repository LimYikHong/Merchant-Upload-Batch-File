package rta.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantCreatedEvent {

    private String merchantId;
    private String merchantName;
    private String merchantBank;
    private String merchantCode;
    private String merchantPhoneNum;
    private String merchantAddress;
    private String merchantContactPerson;
    private String merchantStatus;
    private String createdBy;
    private String createdAt; // ISO string
    private String merchantAccNum;
    private String merchantAccName;
    private String transactionCurrency;
    private String settlementCurrency;
}
