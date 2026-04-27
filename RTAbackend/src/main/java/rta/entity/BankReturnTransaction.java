package rta.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Individual transaction rows from a return batch file sent by the bank.
 */
@Entity
@Table(name = "bank_return_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankReturnTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_batch_id")
    private Long returnBatchId;

    @Column(name = "bank_transaction_id")
    private Long bankTransactionId;

    @Column(name = "merchant_id")
    private String merchantId;

    @Column(name = "merchant_customer")
    private String merchantCustomer;

    @Column(name = "masked_pan")
    private String maskedPan;

    @Column(name = "amount_cents")
    private Long amountCents;

    @Column(name = "currency")
    private String currency;

    /**
     * APPROVED / FAILED / DECLINED
     */
    @Column(name = "status")
    private String status;

    @Column(name = "remark")
    private String remark;

    @Column(name = "authorization_datetime")
    private LocalDateTime authorizationDatetime;
}
