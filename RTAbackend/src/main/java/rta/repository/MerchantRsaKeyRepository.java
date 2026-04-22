package rta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rta.entity.MerchantRsaKey;

import java.util.Optional;

@Repository
public interface MerchantRsaKeyRepository extends JpaRepository<MerchantRsaKey, Long> {

    Optional<MerchantRsaKey> findByMerchantId(String merchantId);

    Optional<MerchantRsaKey> findByMerchantIdAndKeyPurpose(String merchantId, String keyPurpose);
}
