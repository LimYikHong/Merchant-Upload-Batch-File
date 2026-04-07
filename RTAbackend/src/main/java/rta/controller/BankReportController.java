package rta.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import rta.entity.BankSummaryReport;
import rta.entity.MerchantActivityLog;
import rta.entity.RtaBatch;
import rta.repository.BankSummaryReportRepository;
import rta.repository.MerchantActivityLogRepository;
import rta.repository.RtaBatchRepository;

import java.time.LocalDateTime;
import java.util.*;


@CrossOrigin(originPatterns = {"http://localhost:*", "https://localhost:*"})
@RestController
@RequestMapping("/api/reports")
public class BankReportController {

    private final BankSummaryReportRepository reportRepository;
    private final RtaBatchRepository batchRepository;
    private final MerchantActivityLogRepository activityLogRepository;

    public BankReportController(BankSummaryReportRepository reportRepository,
                                RtaBatchRepository batchRepository,
                                MerchantActivityLogRepository activityLogRepository) {
        this.reportRepository = reportRepository;
        this.batchRepository = batchRepository;
        this.activityLogRepository = activityLogRepository;
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
     * GET /api/reports?merchantId=xxx
     * Returns summary reports for a specific merchant, or all reports if no merchantId.
     */
    @GetMapping
    public List<BankSummaryReport> getReports(
            @RequestParam(value = "merchantId", required = false) String merchantId) {
        if (merchantId != null && !merchantId.isEmpty()) {
            return reportRepository.findByMerchantIdOrderByReceivedAtDesc(merchantId);
        }
        return reportRepository.findAll();
    }

    /**
     * GET /api/reports/{id}
     * Returns a single report by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BankSummaryReport> getReportById(@PathVariable Long id) {
        return reportRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/reports/receive
     * Endpoint for the bank to push a summary report after processing a batch file.
     * Accepts JSON payload with batch processing results.
     * Also updates the corresponding batch status to reflect the bank's result.
     */
    @PostMapping("/receive")
    public ResponseEntity<?> receiveReport(@RequestBody BankSummaryReport incomingReport) {
        try {
            // Validate required fields
            if (incomingReport.getMerchantId() == null || incomingReport.getFileName() == null
                    || incomingReport.getBankStatus() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "merchantId, fileName, and bankStatus are required"));
            }

            incomingReport.setReceivedAt(LocalDateTime.now());

            // Try to link with existing batch by batchId or fileName+merchantId
            if (incomingReport.getBatchId() != null) {
                Optional<RtaBatch> batchOpt = batchRepository.findById(incomingReport.getBatchId());
                if (batchOpt.isPresent()) {
                    RtaBatch batch = batchOpt.get();
                    // Update batch status based on bank report
                    batch.setStatus("BANK_" + incomingReport.getBankStatus());
                    batchRepository.save(batch);
                }
            }

            BankSummaryReport saved = reportRepository.save(incomingReport);

            logActivity(incomingReport.getMerchantId(), "REPORT_RECEIVED",
                    "Bank summary report received for file: " + incomingReport.getFileName()
                            + " | Status: " + incomingReport.getBankStatus()
                            + " | Ref: " + (incomingReport.getBankReference() != null
                            ? incomingReport.getBankReference() : "N/A"));

            return ResponseEntity.ok(Map.of(
                    "message", "Report received successfully",
                    "reportId", saved.getId()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process report: " + e.getMessage()));
        }
    }
}
