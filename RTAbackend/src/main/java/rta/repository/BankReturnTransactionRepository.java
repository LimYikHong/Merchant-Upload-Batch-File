package rta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rta.entity.BankReturnTransaction;

import java.util.List;

public interface BankReturnTransactionRepository extends JpaRepository<BankReturnTransaction, Long> {

    List<BankReturnTransaction> findByReturnBatchId(Long returnBatchId);

    List<BankReturnTransaction> findByMerchantId(String merchantId);
}
