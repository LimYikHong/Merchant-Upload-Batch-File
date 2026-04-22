package rta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rta.entity.ReturnBatchFile;

import java.util.List;

@Repository
public interface ReturnBatchFileRepository extends JpaRepository<ReturnBatchFile, Long> {

    List<ReturnBatchFile> findByMerchantIdOrderByReceivedAtDesc(String merchantId);

    List<ReturnBatchFile> findByBatchId(Long batchId);
}
