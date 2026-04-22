package rta.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rta.entity.ReturnBatchFile;
import rta.repository.ReturnBatchFileRepository;
import rta.service.MinioStorageService;

import java.util.List;

/**
 * REST API for merchants to view and download return batch files received from
 * the bank via the internal API.
 */
@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"})
@RestController
@RequestMapping("/api/return-batches")
public class ReturnBatchController {

    private static final Logger log = LoggerFactory.getLogger(ReturnBatchController.class);

    private final ReturnBatchFileRepository returnBatchFileRepository;
    private final MinioStorageService minioStorageService;

    public ReturnBatchController(ReturnBatchFileRepository returnBatchFileRepository,
            MinioStorageService minioStorageService) {
        this.returnBatchFileRepository = returnBatchFileRepository;
        this.minioStorageService = minioStorageService;
    }

    /**
     * GET /api/return-batches?merchantId=xxx List return batch files for a
     * merchant, or all if no merchantId.
     */
    @GetMapping
    public List<ReturnBatchFile> getReturnBatches(
            @RequestParam(value = "merchantId", required = false) String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            return returnBatchFileRepository.findByMerchantIdOrderByReceivedAtDesc(merchantId);
        }
        return returnBatchFileRepository.findAll();
    }

    /**
     * GET /api/return-batches/{id}/download Download the return batch file from
     * MinIO.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<?> downloadReturnBatch(@PathVariable Long id) {
        try {
            ReturnBatchFile returnBatch = returnBatchFileRepository.findById(id)
                    .orElse(null);

            if (returnBatch == null) {
                return ResponseEntity.notFound().build();
            }

            String minioPath = "returns/" + returnBatch.getReturnFileName();
            byte[] fileBytes = minioStorageService.downloadFile(minioPath);

            String downloadName = returnBatch.getOriginalFileName() != null
                    ? returnBatch.getOriginalFileName()
                    : returnBatch.getReturnFileName();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileBytes.length)
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Failed to download return batch file id={}", id, e);
            return ResponseEntity.internalServerError().body("Failed to download file: " + e.getMessage());
        }
    }
}
