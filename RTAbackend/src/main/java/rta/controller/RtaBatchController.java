package rta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import rta.entity.RtaBatch;
import rta.entity.RtaTransaction;
import rta.entity.MerchantActivityLog;
import rta.repository.RtaBatchRepository;
import rta.repository.RtaTransactionRepository;
import rta.repository.MerchantActivityLogRepository;
import rta.service.MinioStorageService;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"})
@RestController
@RequestMapping("/api/batches")
public class RtaBatchController {

    private final RtaBatchRepository batchRepository;
    private final RtaTransactionRepository transactionRepository;
    private final MerchantActivityLogRepository activityLogRepository;
    private final MinioStorageService minioStorageService;

    public RtaBatchController(RtaBatchRepository batchRepository,
            RtaTransactionRepository transactionRepository,
            MerchantActivityLogRepository activityLogRepository,
            MinioStorageService minioStorageService) {
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
        this.activityLogRepository = activityLogRepository;
        this.minioStorageService = minioStorageService;
    }

    private void logActivity(String merchantId, String type, String description) {
        MerchantActivityLog log = new MerchantActivityLog();
        log.setMerchantId(merchantId);
        log.setActivityType(type);
        log.setDescription(description);
        log.setTimestamp(LocalDateTime.now());
        activityLogRepository.save(log);
    }

    /**
     * GET /api/batches - Returns all batches.
     */
    @GetMapping
    public List<RtaBatch> getAllBatches() {
        return batchRepository.findAll();
    }

    /**
     * GET /api/batches/activity - Returns activity messages from DB.
     */
    @GetMapping("/activity")
    public List<String> getActivityLog() {
        return activityLogRepository.findAll().stream()
                .sorted(Comparator.comparing(MerchantActivityLog::getTimestamp).reversed())
                .map(log -> "[" + log.getTimestamp() + "] " + log.getDescription())
                .collect(Collectors.toList());
    }

    /**
     * POST /api/batches/upload - Validates file type + content type. - Saves
     * the uploaded file under /uploads. - Creates a batch record with
     * status=UPLOADED.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadBatch(@RequestParam("file") MultipartFile file,
            @RequestParam("merchantId") String merchantId,
            @RequestParam("originalFileName") String originalFileName) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("No file uploaded");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            String lowerName = fileName.toLowerCase();
            if (!(lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")
                    || lowerName.endsWith(".csv") || lowerName.endsWith(".txt"))) {
                return ResponseEntity.badRequest()
                        .body("Invalid file type. Only .xlsx, .xls, .csv, and .txt are allowed.");
            }

            String contentType = file.getContentType();
            if (contentType == null
                    || !(contentType.equals("application/vnd.ms-excel")
                    || contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    || contentType.equals("text/plain")
                    || contentType.equals("text/csv"))) {
                return ResponseEntity.badRequest()
                        .body("Invalid content type: " + contentType);
            }

            // Upload file to MinIO
            minioStorageService.uploadFile(fileName, file);

            RtaBatch batch = new RtaBatch();
            batch.setOriginalFileName(originalFileName);
            batch.setFileName(fileName);
            batch.setMerchantId(merchantId);
            batch.setCreatedAt(LocalDateTime.now());
            batch.setCreatedBy("system");
            batch.setStatus("UPLOADED");
            RtaBatch savedBatch = batchRepository.save(batch);

            logActivity(merchantId, "UPLOAD_BATCH", "Uploaded: " + fileName + " by " + merchantId);

            return ResponseEntity.ok(savedBatch);

        } catch (Exception e) {
            logActivity(merchantId, "UPLOAD_BATCH_FAILED", "Upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during upload: " + e.getMessage());
        }
    }

    /**
     * Helper: parse CSV into transactions, set batch to READY/FAILED. -
     * Expected columns: accountNumber, amount, currency - Reads file from MinIO
     * storage
     */
    private void processCsvFile(RtaBatch batch, String fileName) {
        try (InputStream is = minioStorageService.downloadFileAsStream(fileName); BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            int success = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    continue;
                }

                RtaTransaction tx = new RtaTransaction();
                tx.setBatch(batch);
                tx.setMerchantId(batch.getMerchantId());
                tx.setAccountNumber(parts[0].trim());
                tx.setAmount(new BigDecimal(parts[1].trim()));
                tx.setCurrency(parts[2].trim());
                tx.setStatus("PENDING");
                tx.setCreatedAt(LocalDateTime.now());
                tx.setCreatedBy("system");

                transactionRepository.save(tx);
                success++;
            }
            batch.setStatus("READY");
            batchRepository.save(batch);
            logActivity(batch.getMerchantId(), "PROCESS_CSV",
                    "Processed CSV: " + fileName + " (" + success + " records)");
        } catch (Exception e) {
            batch.setStatus("FAILED");
            batchRepository.save(batch);
            logActivity(batch.getMerchantId(), "PROCESS_CSV_FAILED",
                    "CSV processing failed for " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Helper: parse first sheet of XLSX into transactions, set batch to
     * READY/FAILED. - Assumes first row is header; skips it. - Expected
     * columns: [0]=accountNumber (string), [1]=amount (numeric), [2]=currency
     * (string). - Reads file from MinIO storage
     */
    private void processExcelFile(RtaBatch batch, String fileName) {
        try (InputStream fis = minioStorageService.downloadFileAsStream(fileName); Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int success = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;
                }

                Cell accCell = row.getCell(0);
                Cell amtCell = row.getCell(1);
                Cell curCell = row.getCell(2);

                if (accCell == null || amtCell == null || curCell == null) {
                    continue;
                }

                RtaTransaction tx = new RtaTransaction();
                tx.setBatch(batch);
                tx.setMerchantId(batch.getMerchantId());
                tx.setAccountNumber(accCell.getStringCellValue().trim());
                tx.setAmount(BigDecimal.valueOf(amtCell.getNumericCellValue()));
                tx.setCurrency(curCell.getStringCellValue().trim());
                tx.setStatus("PENDING");
                tx.setCreatedAt(LocalDateTime.now());
                tx.setCreatedBy("system");

                transactionRepository.save(tx);
                success++;
            }

            batch.setStatus("READY");
            batchRepository.save(batch);
            logActivity(batch.getMerchantId(), "PROCESS_EXCEL",
                    "Processed Excel: " + fileName + " (" + success + " records)");

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batchRepository.save(batch);
            logActivity(batch.getMerchantId(), "PROCESS_EXCEL_FAILED",
                    "Excel processing failed for " + fileName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * POST /api/batches/{id}/send-to-bank - Reads the uploaded file from disk
     * and forwards it to the bank's HTTPS upload API. - Uses SSL trust-all for
     * self-signed certificate (dev only).
     */
    @PostMapping("/{id}/send-to-bank")
    public ResponseEntity<?> sendToBank(@PathVariable Long id) {
        return batchRepository.findById(id).map(batch -> {
            try {
                // Check if file exists in MinIO
                if (!minioStorageService.fileExists(batch.getFileName())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "File not found in storage: " + batch.getFileName()));
                }

                // Create SSL context that trusts all certificates (for self-signed dev cert)
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
                };
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                        .sslContext(sslContext)
                        .build();

                // Build multipart form data
                String boundary = "----FormBoundary" + System.currentTimeMillis();
                // Download file from MinIO
                byte[] fileBytes = minioStorageService.downloadFile(batch.getFileName());
                String fileName = batch.getFileName();
                String originalFileName = batch.getOriginalFileName();
                String merchantId = batch.getMerchantId();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                String filePart = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n";
                baos.write(filePart.getBytes());
                baos.write(fileBytes);
                baos.write("\r\n".getBytes());

                String merchantPart = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"merchantId\"\r\n\r\n"
                        + merchantId + "\r\n";
                baos.write(merchantPart.getBytes());

                // Include file name (renamed with merchantId + timestamp)
                String fileNamePart = "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"fileName\"\r\n\r\n"
                        + fileName + "\r\n";
                baos.write(fileNamePart.getBytes());

                // Include original file name for audit trail
                if (originalFileName != null && !originalFileName.isEmpty()) {
                    String originalFileNamePart = "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\"originalFileName\"\r\n\r\n"
                            + originalFileName + "\r\n";
                    baos.write(originalFileNamePart.getBytes());
                }

                baos.write(("--" + boundary + "--\r\n").getBytes());

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create("https://localhost:8086/api/incoming/upload"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray()))
                        .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                        java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    batch.setStatus("SENT_TO_BANK");
                    batchRepository.save(batch);
                    logActivity(merchantId, "SEND_TO_BANK",
                            "Sent " + fileName + " to bank successfully");
                    return ResponseEntity.ok(Map.of(
                            "message", "File sent to bank successfully",
                            "bankResponse", response.body()));
                } else {
                    logActivity(merchantId, "SEND_TO_BANK_FAILED",
                            "Bank returned status " + response.statusCode() + ": " + response.body());
                    return ResponseEntity.status(response.statusCode())
                            .body(Map.of("error", "Bank rejected the file", "details", response.body()));
                }

            } catch (Exception e) {
                logActivity(batch.getMerchantId(), "SEND_TO_BANK_FAILED",
                        "Failed to send to bank: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Failed to send file to bank: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/batches/{id} - Updates batch fields (currently
     * merchantId/status).
     */
    @PutMapping("/{id}")
    public ResponseEntity<RtaBatch> updateBatch(@PathVariable Long id, @RequestBody RtaBatch batchDetails) {
        return batchRepository.findById(id).map(batch -> {
            if (batchDetails.getMerchantId() != null) {
                batch.setMerchantId(batchDetails.getMerchantId());
            }
            if (batchDetails.getStatus() != null) {
                batch.setStatus(batchDetails.getStatus());
            }
            RtaBatch updated = batchRepository.save(batch);
            logActivity(batch.getMerchantId(), "UPDATE_BATCH", "Updated batch ID " + id);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/batches/{id} - Deletes related transactions, the file on disk
     * (if present), and the batch record itself. - Returns success JSON or an
     * error message if file deletion fails after DB delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBatch(@PathVariable Long id) {
        return batchRepository.findById(id).map(batch -> {
            try {
                List<RtaTransaction> transactions = transactionRepository.findByBatchId(id);
                if (!transactions.isEmpty()) {
                    transactionRepository.deleteAll(transactions);
                    logActivity(batch.getMerchantId(), "DELETE_TRANSACTIONS",
                            "Deleted " + transactions.size() + " transactions for batch " + id);
                }

                // Delete file from MinIO
                if (minioStorageService.fileExists(batch.getFileName())) {
                    minioStorageService.deleteFile(batch.getFileName());
                    logActivity(batch.getMerchantId(), "DELETE_FILE", "Deleted file from MinIO: " + batch.getFileName());
                }

                batchRepository.delete(batch);
                logActivity(batch.getMerchantId(), "DELETE_BATCH", "Batch ID " + id + " deleted.");

                return ResponseEntity.ok(Map.of("message", "Batch and related records deleted successfully"));
            } catch (Exception e) {
                logActivity(batch.getMerchantId(), "DELETE_BATCH_FAILED",
                        "File deletion failed for batch " + id + ": " + e.getMessage());
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Batch deleted from DB, but file removal failed"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
