package rta.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import rta.entity.ExternalMerchantInfo;

public interface ExternalMerchantInfoRepository extends JpaRepository<ExternalMerchantInfo, Long> {

    Optional<ExternalMerchantInfo> findByMerchantId(String merchantId);
}
