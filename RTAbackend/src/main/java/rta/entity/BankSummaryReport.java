package rta.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_summary_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankSummaryReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Batch ID from bank system (NOT a FK to rta_batches)
     */
    @Column(name = "bank_batch_id")
    private Long batchId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name")
    private String originalFileName;

    /**
     * Bank's processing status: PROCESSED, PARTIALLY_PROCESSED, REJECTED, etc.
     */
    @Column(name = "bank_status", nullable = false)
    private String bankStatus;

    /**
     * Total number of transactions in the file
     */
    @Column(name = "total_transactions")
    private Integer totalTransactions;

    /**
     * Number of transactions successfully processed by bank
     */
    @Column(name = "successful_transactions")
    private Integer successfulTransactions;

    /**
     * Number of transactions that failed bank processing
     */
    @Column(name = "failed_transactions")
    private Integer failedTransactions;

    /**
     * Total amount processed by bank
     */
    @Column(name = "total_amount")
    private java.math.BigDecimal totalAmount;

    /**
     * Currency of the transactions
     */
    @Column(name = "currency")
    private String currency;

    /**
     * Bank's reference/tracking number for this batch
     */
    @Column(name = "bank_reference")
    private String bankReference;

    /**
     * Any remarks or error details from the bank
     */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    /**
     * When the bank processed this file
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * When this report was received by merchant system
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
