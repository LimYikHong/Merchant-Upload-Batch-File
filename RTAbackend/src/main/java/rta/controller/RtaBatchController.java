package rta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import rta.entity.RtaBatch;
import rta.entity.RtaTransaction;
import rta.repository.RtaBatchRepository;
import rta.repository.RtaTransactionRepository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import org.apache.commons.io.FilenameUtils;

import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/batches")
public class RtaBatchController {

    private final RtaBatchRepository batchRepository;
    private final RtaTransactionRepository transactionRepository;

    private final List<String> activityLog = new ArrayList<>();

    public RtaBatchController(RtaBatchRepository batchRepository,
            RtaTransactionRepository transactionRepository) {
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<RtaBatch> getAllBatches() {
        return batchRepository.findAll();
    }

    @GetMapping("/activity")
    public List<String> getActivityLog() {
        return activityLog;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadBatch(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "merchantId", required = false) String merchantId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("❌ No file uploaded");
            }

            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            List<String> supportedExtensions = Arrays.asList("csv", "txt", "xlsx");

            if (!supportedExtensions.contains(extension.toLowerCase())) {
                return ResponseEntity.badRequest()
                        .body("Unsupported file type. Please upload .csv, .txt, or .xlsx files.");
            }

            String uploadDir = "uploads/";
            Files.createDirectories(Paths.get(uploadDir));

            Path path = Paths.get(uploadDir + originalFilename);

            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            RtaBatch batch = new RtaBatch();
            batch.setOriginalFileName(originalFilename);
            batch.setFileName(originalFilename);
            batch.setMerchantId(merchantId != null ? merchantId : "UNKNOWN");
            batch.setCreatedAt(LocalDateTime.now());
            batch.setCreatedBy("system");
            batch.setStatus("UPLOADED");
            batchRepository.save(batch);

            String fileName = originalFilename.toLowerCase();

            if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                processCsvFile(batch, path.toFile());
            } else if (fileName.endsWith(".xlsx")) {
                processExcelFile(batch, path.toFile());
            } else {
                batch.setStatus("UNSUPPORTED");
                batchRepository.save(batch);
                activityLog.add("❌ Unsupported file type: " + fileName);
                return ResponseEntity.badRequest().body("Unsupported file type");
            }

            activityLog.add("✅ Upload success: " + fileName + " by " + merchantId);
            return ResponseEntity.ok(Map.of(
                    "message", "Upload successful",
                    "fileName", originalFilename,
                    "status", "READY"));

        } catch (Exception e) {
            activityLog.add("❌ Upload failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error during upload: " + e.getMessage());
        }
    }

    private void processCsvFile(RtaBatch batch, File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int success = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3)
                    continue;

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
            activityLog.add("📄 Processed CSV: " + file.getName() + " (" + success + " records)");
        } catch (Exception e) {
            batch.setStatus("FAILED");
            batchRepository.save(batch);
            activityLog.add("❌ CSV processing failed for " + file.getName() + ": " + e.getMessage());
        }
    }

    private void processExcelFile(RtaBatch batch, File file) {
        try (InputStream fis = new FileInputStream(file);
                Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            int success = 0;

            for (Row row : sheet) {
                if (row.getRowNum() == 0)
                    continue; // Skip header

                Cell accCell = row.getCell(0);
                Cell amtCell = row.getCell(1);
                Cell curCell = row.getCell(2);

                if (accCell == null || amtCell == null || curCell == null)
                    continue;

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
            activityLog.add("📊 Processed Excel: " + file.getName() + " (" + success + " records)");

        } catch (Exception e) {
            batch.setStatus("FAILED");
            batchRepository.save(batch);
            activityLog.add("❌ Excel processing failed for " + file.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<RtaBatch> updateBatch(@PathVariable Long id, @RequestBody RtaBatch batchDetails) {
        return batchRepository.findById(id).map(batch -> {
            if (batchDetails.getMerchantId() != null)
                batch.setMerchantId(batchDetails.getMerchantId());
            if (batchDetails.getStatus() != null)
                batch.setStatus(batchDetails.getStatus());
            RtaBatch updated = batchRepository.save(batch);
            activityLog.add("✏️ Updated batch ID " + id);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBatch(@PathVariable Long id) {
        return batchRepository.findById(id).map(batch -> {
            transactionRepository.deleteAll(transactionRepository.findByBatchId(id));
            batchRepository.delete(batch);
            activityLog.add("🗑️ Batch ID " + id + " deleted.");
            return ResponseEntity.ok().body(Map.of("message", "Batch deleted successfully"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
