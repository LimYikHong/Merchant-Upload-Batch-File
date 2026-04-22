package rta.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import rta.entity.BankSummaryReport;
import rta.entity.MerchantActivityLog;
import rta.entity.MerchantRsaKey;
import rta.entity.ReturnBatchFile;
import rta.entity.RtaBatch;
import rta.repository.BankSummaryReportRepository;
import rta.repository.MerchantActivityLogRepository;
import rta.repository.MerchantRsaKeyRepository;
import rta.repository.ReturnBatchFileRepository;
import rta.repository.RtaBatchRepository;
import rta.service.MinioStorageService;
import rta.service.ReturnFileDecryptionService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
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
    private final BankSummaryReportRepository reportRepository;
    private final RtaBatchRepository batchRepository;
    private final MerchantActivityLogRepository activityLogRepository;
    private final MinioStorageService minioStorageService;
    private final ReturnFileDecryptionService decryptionService;

    public InternalApiController(MerchantRsaKeyRepository rsaKeyRepository,
            ReturnBatchFileRepository returnBatchFileRepository,
            BankSummaryReportRepository reportRepository,
            RtaBatchRepository batchRepository,
            MerchantActivityLogRepository activityLogRepository,
            MinioStorageService minioStorageService,
            ReturnFileDecryptionService decryptionService) {
        this.rsaKeyRepository = rsaKeyRepository;
        this.returnBatchFileRepository = returnBatchFileRepository;
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
            @RequestParam(value = "batchId", required = false) Long batchId,
            @RequestParam(value = "originalFileName", required = false) String originalFileName,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "encryptedAesKey", required = false) String encryptedAesKey,
            @RequestParam(value = "iv", required = false) String iv,
            @RequestParam(value = "encrypted", required = false, defaultValue = "false") boolean encrypted) {

        log.info("Internal API: batch return received for merchant {}, batchId={}", merchantId, batchId);

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Generate return file name: return_{merchantId}_{timestamp}.{ext}
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String origName = file.getOriginalFilename();
            String ext = (origName != null && origName.contains("."))
                    ? origName.substring(origName.lastIndexOf('.'))
                    : ".dat";
            String returnFileName = "return_" + merchantId + "_" + timestamp + ext;

            // Store in MinIO under a "returns/" prefix
            byte[] fileBytes = file.getBytes();

            // If encrypted, decrypt with our RSA private key + AES
            if (encrypted && encryptedAesKey != null && iv != null) {
                log.info("Decrypting return batch file for merchant {}", merchantId);
                fileBytes = decryptionService.decrypt(merchantId, fileBytes, encryptedAesKey, iv);
            }

            minioStorageService.uploadFile("returns/" + returnFileName, fileBytes,
                    file.getContentType());

            // Save record
            ReturnBatchFile returnBatch = new ReturnBatchFile();
            returnBatch.setBatchId(batchId);
            returnBatch.setMerchantId(merchantId);
            returnBatch.setOriginalFileName(originalFileName);
            returnBatch.setReturnFileName(returnFileName);
            returnBatch.setFileSize((long) fileBytes.length);
            returnBatch.setStatus("RECEIVED");
            returnBatch.setRemarks(remarks);
            returnBatch.setReceivedAt(LocalDateTime.now());
            ReturnBatchFile saved = returnBatchFileRepository.save(returnBatch);

            // Update original batch status if batchId is provided
            if (batchId != null) {
                batchRepository.findById(batchId).ifPresent(batch -> {
                    batch.setStatus("RETURN_RECEIVED");
                    batchRepository.save(batch);
                });
            }

            logActivity(merchantId, "BATCH_RETURN_RECEIVED",
                    "Return batch file received: " + returnFileName
                    + (batchId != null ? " (batchId=" + batchId + ")" : ""));

            return ResponseEntity.ok(Map.of(
                    "message", "Return batch file received successfully",
                    "returnFileId", saved.getId(),
                    "returnFileName", returnFileName));

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
    public ResponseEntity<?> receiveReport(@RequestBody BankSummaryReport incomingReport) {
        log.info("Internal API: report received for merchant {}, file {}",
                incomingReport.getMerchantId(), incomingReport.getFileName());

        try {
            if (incomingReport.getMerchantId() == null || incomingReport.getFileName() == null
                    || incomingReport.getBankStatus() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "merchantId, fileName, and bankStatus are required"));
            }

            incomingReport.setReceivedAt(LocalDateTime.now());

            // Update corresponding batch status if batchId provided
            if (incomingReport.getBatchId() != null) {
                batchRepository.findById(incomingReport.getBatchId()).ifPresent(batch -> {
                    batch.setStatus("BANK_" + incomingReport.getBankStatus());
                    batchRepository.save(batch);
                });
            }

            BankSummaryReport saved = reportRepository.save(incomingReport);

            logActivity(incomingReport.getMerchantId(), "REPORT_RECEIVED",
                    "Summary report received via internal API for file: " + incomingReport.getFileName()
                    + " | Status: " + incomingReport.getBankStatus()
                    + " | Ref: " + (incomingReport.getBankReference() != null
                    ? incomingReport.getBankReference() : "N/A"));

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
}
