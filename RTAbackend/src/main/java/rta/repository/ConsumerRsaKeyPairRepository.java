package rta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rta.entity.ConsumerRsaKeyPair;

import java.util.Optional;

@Repository
public interface ConsumerRsaKeyPairRepository extends JpaRepository<ConsumerRsaKeyPair, Long> {

    /**
     * Get the latest key pair (most recently created).
     */
    Optional<ConsumerRsaKeyPair> findTopByOrderByCreatedAtDesc();
}
