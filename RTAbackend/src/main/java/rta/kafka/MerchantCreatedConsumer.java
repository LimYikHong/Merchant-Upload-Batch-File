package rta.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import rta.model.MerchantProfile;
import rta.entity.MerchantRsaKey;
import rta.repository.ProfileRepository;
import rta.repository.MerchantRsaKeyRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MerchantCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerchantCreatedConsumer.class);
    private final ProfileRepository profileRepository;
    private final MerchantRsaKeyRepository rsaKeyRepository;

    public MerchantCreatedConsumer(ProfileRepository profileRepository,
            MerchantRsaKeyRepository rsaKeyRepository) {
        this.profileRepository = profileRepository;
        this.rsaKeyRepository = rsaKeyRepository;
    }

    @KafkaListener(topics = "${rta.kafka.topic.merchant-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload MerchantCreatedEvent event) {
        log.info("Received merchant-created event: merchantId={}", event.getMerchantId());

        // Create merchant profile if it doesn't already exist
        if (profileRepository.findByMerchantId(event.getMerchantId()).isPresent()) {
            log.info("Merchant already exists, skipping profile creation: merchantId={}", event.getMerchantId());
        } else {
            MerchantProfile profile = new MerchantProfile();
            profile.setMerchantId(event.getMerchantId());
            profile.setName(event.getName());
            profile.setUsername(event.getUsername() != null ? event.getUsername() : event.getMerchantId());
            profile.setPassword("123456");              // default password, merchant should change
            profile.setAddress(event.getAddress());
            profile.setPhone(event.getPhone());
            profile.setContact(event.getContact());
            profile.setCompany(event.getCompany());

            try {
                profile.setJoinedOn(LocalDateTime.parse(event.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME));
            } catch (Exception ex) {
                profile.setJoinedOn(LocalDateTime.now());
            }

            profileRepository.save(profile);
            log.info("Inserted merchant into rta_user: merchantId={}", event.getMerchantId());
        }

        // Save INBOUND public key (merchant uses this to encrypt batch uploads)
        if (event.getRsaPublicKeyPem() != null && !event.getRsaPublicKeyPem().isEmpty()) {
            if (rsaKeyRepository.findByMerchantIdAndKeyPurpose(event.getMerchantId(), "INBOUND").isEmpty()) {
                MerchantRsaKey inboundKey = new MerchantRsaKey();
                inboundKey.setMerchantId(event.getMerchantId());
                inboundKey.setKeyPurpose("INBOUND");
                inboundKey.setRsaPublicKey(event.getRsaPublicKeyPem());
                inboundKey.setCreatedAt(LocalDateTime.now());
                rsaKeyRepository.save(inboundKey);
                log.info("Saved INBOUND public key for merchant: {}", event.getMerchantId());
            } else {
                log.info("INBOUND key already exists for merchant: {}", event.getMerchantId());
            }
        } else {
            log.warn("No INBOUND public key in event for merchant: {}", event.getMerchantId());
        }

        // Save OUTBOUND private key (merchant uses this to decrypt return files from bank)
        if (event.getRsaOutboundPrivateKeyPem() != null && !event.getRsaOutboundPrivateKeyPem().isEmpty()) {
            if (rsaKeyRepository.findByMerchantIdAndKeyPurpose(event.getMerchantId(), "OUTBOUND").isEmpty()) {
                MerchantRsaKey outboundKey = new MerchantRsaKey();
                outboundKey.setMerchantId(event.getMerchantId());
                outboundKey.setKeyPurpose("OUTBOUND");
                outboundKey.setRsaPublicKey(event.getRsaOutboundPrivateKeyPem());
                outboundKey.setCreatedAt(LocalDateTime.now());
                rsaKeyRepository.save(outboundKey);
                log.info("Saved OUTBOUND private key for merchant: {}", event.getMerchantId());
            } else {
                log.info("OUTBOUND key already exists for merchant: {}", event.getMerchantId());
            }
        } else {
            log.warn("No OUTBOUND private key in event for merchant: {}", event.getMerchantId());
        }
    }
}
