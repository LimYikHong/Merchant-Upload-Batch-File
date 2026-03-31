package rta.repository;

import rta.entity.BankSummaryReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankSummaryReportRepository extends JpaRepository<BankSummaryReport, Long> {

    List<BankSummaryReport> findByMerchantIdOrderByReceivedAtDesc(String merchantId);

    Optional<BankSummaryReport> findByBatchId(Long batchId);

    List<BankSummaryReport> findByBankStatus(String bankStatus);
}
