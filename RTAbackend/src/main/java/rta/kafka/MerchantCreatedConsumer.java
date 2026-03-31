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

        // Save RSA public key if provided in the event (always attempt, even if merchant existed)
        if (event.getRsaPublicKeyPem() != null && !event.getRsaPublicKeyPem().isEmpty()) {
            if (rsaKeyRepository.findByMerchantId(event.getMerchantId()).isEmpty()) {
                MerchantRsaKey rsaKey = new MerchantRsaKey();
                rsaKey.setMerchantId(event.getMerchantId());
                rsaKey.setRsaPublicKey(event.getRsaPublicKeyPem());
                rsaKey.setCreatedAt(LocalDateTime.now());
                rsaKeyRepository.save(rsaKey);
                log.info("Saved RSA public key for merchant: {}", event.getMerchantId());
            } else {
                log.info("RSA key already exists for merchant: {}", event.getMerchantId());
            }
        } else {
            log.warn("No RSA public key in event for merchant: {}", event.getMerchantId());
        }
    }
}