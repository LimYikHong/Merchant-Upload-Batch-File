package rta.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores return/response batch files sent back by the bank after processing.
 */
@Entity
@Table(name = "bank_return_batches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnBatchFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Batch ID from bank system (NOT a FK)
     */
    @Column(name = "bank_batch_id")
    private Long batchId;

    @Column(name = "merchant_id", nullable = false)
    private String merchantId;

    /**
     * Original uploaded file name
     */
    @Column(name = "original_file_name")
    private String originalFileName;

    /**
     * Stored file name in MinIO (return file)
     */
    @Column(name = "return_file_name", nullable = false)
    private String returnFileName;

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Local path or storage URI of decrypted CSV
     */
    @Column(name = "return_file_path")
    private String returnFilePath;

    /**
     * Number of transactions in the return file
     */
    @Column(name = "transaction_count")
    private Integer transactionCount;

    /**
     * Status: RECEIVED, PROCESSED, ERROR
     */
    @Column(name = "status", nullable = false)
    private String status;

    /**
     * Any remarks from the bank
     */
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
