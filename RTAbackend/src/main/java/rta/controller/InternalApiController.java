package rta.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import rta.entity.BankReturnTransaction;
import rta.entity.BankSummaryReport;
import rta.entity.MerchantActivityLog;
import rta.entity.MerchantRsaKey;
import rta.entity.ReturnBatchFile;
import rta.entity.RtaBatch;
import rta.repository.BankReturnTransactionRepository;
import rta.repository.BankSummaryReportRepository;
import rta.repository.MerchantActivityLogRepository;
import rta.repository.MerchantRsaKeyRepository;
import rta.repository.ReturnBatchFileRepository;
import rta.repository.RtaBatchRepository;
import rta.service.MinioStorageService;
import rta.service.ReturnFileDecryptionService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Internal API endpoints called by the main system (RTA_BANK). Secured by
 * InternalApiKeyFilter (X-API-Key header required).
 *
 * Endpoints: GET /api/internal/public-key/{merchantId} - Serve RSA public key
 * POST /api/internal/batch-return - Receive return batch file POST
 * /api/internal/report - Receive summary report
 */
@RestController
@RequestMapping("/api/internal")
public class InternalApiController {

    private static final Logger log = LoggerFactory.getLogger(InternalApiController.class);

    private final MerchantRsaKeyRepository rsaKeyRepository;
    private final ReturnBatchFileRepository returnBatchFileRepository;
    private final BankReturnTransactionRepository returnTransactionRepository;
    private final BankSummaryReportRepository reportRepository;
    private final RtaBatchRepository batchRepository;
    private final MerchantActivityLogRepository activityLogRepository;
    private final MinioStorageService minioStorageService;
    private final ReturnFileDecryptionService decryptionService;

    public InternalApiController(MerchantRsaKeyRepository rsaKeyRepository,
            ReturnBatchFileRepository returnBatchFileRepository,
            BankReturnTransactionRepository returnTransactionRepository,
            BankSummaryReportRepository reportRepository,
            RtaBatchRepository batchRepository,
            MerchantActivityLogRepository activityLogRepository,
            MinioStorageService minioStorageService,
            ReturnFileDecryptionService decryptionService) {
        this.rsaKeyRepository = rsaKeyRepository;
        this.returnBatchFileRepository = returnBatchFileRepository;
        this.returnTransactionRepository = returnTransactionRepository;
        this.reportRepository = reportRepository;
        this.batchRepository = batchRepository;
        this.activityLogRepository = activityLogRepository;
        this.minioStorageService = minioStorageService;
        this.decryptionService = decryptionService;
    }

    // ──────────────────────────────────────────────
    //  1. GET /api/internal/public-key/{merchantId}
    //     Main system calls this to retrieve the merchant's INBOUND RSA public key.
    //     This is the key the merchant uses to encrypt batch uploads.
    // ──────────────────────────────────────────────
    @GetMapping("/public-key/{merchantId}")
    public ResponseEntity<?> getPublicKey(@PathVariable String merchantId) {
        log.info("Internal API: INBOUND public key requested for merchant {}", merchantId);

        Optional<MerchantRsaKey> rsaKeyOpt = rsaKeyRepository.findByMerchantIdAndKeyPurpose(merchantId, "INBOUND");
        if (rsaKeyOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MerchantRsaKey rsaKey = rsaKeyOpt.get();
        return ResponseEntity.ok(Map.of(
                "merchantId", merchantId,
                "publicKey", rsaKey.getRsaPublicKey(),
                "createdAt", rsaKey.getCreatedAt().toString()));
    }

    // ──────────────────────────────────────────────
    //  2. POST /api/internal/batch-return
    //     Main system sends back processed/return batch files.
    //     Expects multipart: file + merchantId + optional batchId, originalFileName, remarks
    // ──────────────────────────────────────────────
    @PostMapping("/batch-return")
    public ResponseEntity<?> receiveBatchReturn(
            @RequestParam("file") MultipartFile file,
            @RequestParam("merchantId") String merchantId,
            @RequestParam(value = "batchId", required = false) String batchIdStr,
            @RequestParam(value = "originalFileName", required = false) String originalFileName,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "encryptedAesKey", required = false) String encryptedAesKey,
            @RequestParam(value = "iv", required = false) String iv,
            @RequestParam(value = "encrypted", required = false, defaultValue = "false") boolean encrypted,
            @RequestParam(value = "transactionCount", required = false) String transactionCount,
            @RequestParam(value = "producerPublicKey", required = false) String producerPublicKey) {

        Long batchId = null;
        if (batchIdStr != null && !batchIdStr.isEmpty()) {
            try {
                batchId = Long.valueOf(batchIdStr);
            } catch (NumberFormatException e) {
                log.warn("Invalid batchId '{}', ignoring", batchIdStr);
            }
        }

        log.info("Internal API: batch return received for merchant {}, batchId={}, encrypted={}, hasAesKey={}, hasIv={}",
                merchantId, batchId, encrypted,
                encryptedAesKey != null && !encryptedAesKey.isEmpty(),
                iv != null && !iv.isEmpty());

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Generate return file name: return_{merchantId}_{timestamp}.csv
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String returnFileName = "return_" + merchantId + "_" + timestamp + ".csv";

            byte[] fileBytes = file.getBytes();

            // Auto-detect encryption: if encryptedAesKey and iv are provided, decrypt
            boolean isEncrypted = (encryptedAesKey != null && !encryptedAesKey.isEmpty()
                    && iv != null && !iv.isEmpty()) || encrypted;
            if (isEncrypted && encryptedAesKey != null && iv != null) {
                log.info("Decrypting return batch file for merchant {} (encrypted={})", merchantId, isEncrypted);
                fileBytes = decryptionService.decrypt(merchantId, fileBytes, encryptedAesKey, iv);
                log.info("Decrypted return batch file: {} bytes", fileBytes.length);
            }

            // Store decrypted file in MinIO under "returns/" prefix
            minioStorageService.uploadFile("returns/" + returnFileName, fileBytes,
                    "text/csv");

            // Save record
            ReturnBatchFile returnBatch = new ReturnBatchFile();
            returnBatch.setBatchId(batchId);
            returnBatch.setMerchantId(merchantId);
            returnBatch.setOriginalFileName(originalFileName);
            returnBatch.setReturnFileName(returnFileName);
            returnBatch.setReturnFilePath("minio://returns/" + returnFileName);
            returnBatch.setFileSize((long) fileBytes.length);
            returnBatch.setStatus("RECEIVED");
            returnBatch.setRemarks(remarks);
            returnBatch.setReceivedAt(LocalDateTime.now());
            if (transactionCount != null) {
                try {
                    returnBatch.setTransactionCount(Integer.parseInt(transactionCount));
                } catch (NumberFormatException ignored) {
                }
            }
            ReturnBatchFile saved = returnBatchFileRepository.save(returnBatch);

            // Parse decrypted CSV rows into bank_return_transactions
            try {
                List<BankReturnTransaction> txns = parseCsvTransactions(fileBytes, saved.getId(), merchantId);
                if (!txns.isEmpty()) {
                    returnTransactionRepository.saveAll(txns);
                    log.info("Saved {} return transactions for batch return {}", txns.size(), saved.getId());
                    if (returnBatch.getTransactionCount() == null) {
                        returnBatch.setTransactionCount(txns.size());
                        returnBatchFileRepository.save(returnBatch);
                    }
                }
            } catch (Exception csvEx) {
                log.warn("Failed to parse CSV transactions: {}", csvEx.getMessage());
            }

            // Update original batch status if batchId is provided
            if (batchId != null) {
                batchRepository.findById(batchId).ifPresent(batch -> {
                    batch.setStatus("RETURN_RECEIVED");
                    batchRepository.save(batch);
                });
            }

            logActivity(merchantId, "BATCH_RETURN_RECEIVED",
                    "Return batch file received: " + returnFileName
                    + (batchId != null ? " (batchId=" + batchId + ")" : "")
                    + (transactionCount != null ? " | txnCount=" + transactionCount : ""));

            return ResponseEntity.ok(Map.of("message", "Return batch received"));

        } catch (Exception e) {
            log.error("Failed to process return batch file", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process return file: " + e.getMessage()));
        }
    }

    // ──────────────────────────────────────────────
    //  3. POST /api/internal/report
    //     Main system sends summary report after batch processing.
    //     Accepts JSON payload.
    // ──────────────────────────────────────────────
    @PostMapping("/report")
    public ResponseEntity<?> receiveReport(@RequestBody Map<String, Object> payload) {
        log.info("Internal API: report received, payload keys: {}", payload.keySet());
        log.info("Internal API: report payload: {}", payload);

        try {
            String merchantId = (String) payload.get("merchantId");
            Object batchIdObj = payload.get("batchId");
            Long batchId = null;
            if (batchIdObj != null) {
                try {
                    batchId = Long.valueOf(batchIdObj.toString());
                } catch (NumberFormatException e) {
                    log.warn("Invalid batchId '{}', ignoring", batchIdObj);
                }
            }

            if (merchantId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "merchantId is required"));
            }

            // Check if this is an encrypted report
            String encryptedContent = (String) payload.get("encryptedContent");
            String encryptedAesKey = (String) payload.get("encryptedAesKey");
            String ivStr = (String) payload.get("iv");

            Map<String, Object> reportData;

            if (encryptedContent != null && encryptedAesKey != null && ivStr != null) {
                // Decrypt the report content
                log.info("Decrypting encrypted report for merchant {}", merchantId);
                byte[] encryptedBytes = java.util.Base64.getDecoder().decode(encryptedContent);
                byte[] decryptedBytes = decryptionService.decrypt(merchantId, encryptedBytes, encryptedAesKey, ivStr);
                String decryptedJson = new String(decryptedBytes, StandardCharsets.UTF_8);
                log.info("Decrypted report JSON: {}", decryptedJson);

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                reportData = mapper.readValue(decryptedJson, Map.class);
            } else {
                // Plain (unencrypted) report — use the payload directly
                reportData = payload;
            }

            // Map decrypted fields to BankSummaryReport
            BankSummaryReport report = new BankSummaryReport();
            report.setBatchId(batchId);
            report.setMerchantId(merchantId);
            report.setFileName((String) reportData.getOrDefault("fileName", ""));
            report.setOriginalFileName((String) reportData.get("originalFileName"));
            report.setBankStatus((String) reportData.getOrDefault("batchStatus",
                    reportData.getOrDefault("bankStatus", "UNKNOWN")));

            // Map transaction counts
            Object totalCount = reportData.get("totalCount");
            Object approvedCount = reportData.get("approvedCount");
            Object declinedCount = reportData.get("declinedCount");
            Object failedCount = reportData.get("failedCount");

            if (totalCount != null) {
                report.setTotalTransactions(Integer.valueOf(totalCount.toString()));
            }
            if (approvedCount != null) {
                report.setSuccessfulTransactions(Integer.valueOf(approvedCount.toString()));
            }
            // Failed = declined + failed
            int totalFailed = 0;
            if (declinedCount != null) {
                totalFailed += Integer.parseInt(declinedCount.toString());
            }
            if (failedCount != null) {
                totalFailed += Integer.parseInt(failedCount.toString());
            }
            if (declinedCount != null || failedCount != null) {
                report.setFailedTransactions(totalFailed);
            }

            // Parse processedAt
            String processedAtStr = (String) reportData.get("processedAt");
            if (processedAtStr != null) {
                try {
                    report.setProcessedAt(LocalDateTime.parse(processedAtStr));
                } catch (Exception ex) {
                    log.warn("Could not parse processedAt: {}", processedAtStr);
                }
            }

            // Build remarks from transaction summary
            StringBuilder remarksBuilder = new StringBuilder();
            if (approvedCount != null) {
                remarksBuilder.append("Approved: ").append(approvedCount);
            }
            if (declinedCount != null) {
                remarksBuilder.append(", Declined: ").append(declinedCount);
            }
            if (failedCount != null) {
                remarksBuilder.append(", Failed: ").append(failedCount);
            }
            if (remarksBuilder.length() > 0) {
                report.setRemarks(remarksBuilder.toString());
            }

            report.setReceivedAt(LocalDateTime.now());

            // Update corresponding batch status
            if (batchId != null) {
                batchRepository.findById(batchId).ifPresent(batch -> {
                    batch.setStatus("BANK_" + report.getBankStatus());
                    batchRepository.save(batch);
                });
            }

            BankSummaryReport saved = reportRepository.save(report);

            logActivity(merchantId, "REPORT_RECEIVED",
                    "Summary report received via internal API for file: " + report.getFileName()
                    + " | Status: " + report.getBankStatus()
                    + " | Total: " + (totalCount != null ? totalCount : "N/A")
                    + " | Approved: " + (approvedCount != null ? approvedCount : "N/A"));

            return ResponseEntity.ok(Map.of(
                    "message", "Report received successfully",
                    "reportId", saved.getId()));

        } catch (Exception e) {
            log.error("Failed to process report via internal API", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process report: " + e.getMessage()));
        }
    }

    private void logActivity(String merchantId, String type, String description) {
        MerchantActivityLog logEntry = new MerchantActivityLog();
        logEntry.setMerchantId(merchantId);
        logEntry.setActivityType(type);
        logEntry.setDescription(description);
        logEntry.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(logEntry);
    }

    /**
     * Parse decrypted CSV into BankReturnTransaction rows. Expected CSV columns
     * (header row): transactionId, merchantCustomer, maskedPan, amount,
     * currency, status, remark, authorizationDatetime
     */
    private List<BankReturnTransaction> parseCsvTransactions(byte[] csvBytes, Long returnBatchId, String merchantId) {
        List<BankReturnTransaction> txns = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine(); // skip header
            if (headerLine == null) {
                return txns;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] cols = line.split(",", -1);
                BankReturnTransaction txn = new BankReturnTransaction();
                txn.setReturnBatchId(returnBatchId);
                txn.setMerchantId(merchantId);

                if (cols.length > 0 && !cols[0].isEmpty()) {
                    try {
                        txn.setBankTransactionId(Long.parseLong(cols[0].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (cols.length > 1) {
                    txn.setMerchantCustomer(cols[1].trim());
                }
                if (cols.length > 2) {
                    txn.setMaskedPan(cols[2].trim());
                }
                if (cols.length > 3 && !cols[3].isEmpty()) {
                    try {
                        txn.setAmountCents(Long.parseLong(cols[3].trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (cols.length > 4) {
                    txn.setCurrency(cols[4].trim());
                }
                if (cols.length > 5) {
                    txn.setStatus(cols[5].trim());
                }
                if (cols.length > 6) {
                    txn.setRemark(cols[6].trim());
                }
                if (cols.length > 7 && !cols[7].trim().isEmpty()) {
                    try {
                        txn.setAuthorizationDatetime(LocalDateTime.parse(cols[7].trim()));
                    } catch (Exception ignored) {
                    }
                }

                txns.add(txn);
            }
        } catch (Exception e) {
            log.warn("CSV parsing error: {}", e.getMessage());
        }
        return txns;
    }
}
