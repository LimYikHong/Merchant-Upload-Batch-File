package rta.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import rta.model.MerchantProfile;
import rta.repository.ProfileRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MerchantCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerchantCreatedConsumer.class);
    private final ProfileRepository profileRepository;

    public MerchantCreatedConsumer(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @KafkaListener(topics = "${rta.kafka.topic.merchant-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload MerchantCreatedEvent event) {
        log.info("Received merchant-created event: merchantId={}", event.getMerchantId());

        // Skip if merchant already exists
        if (profileRepository.findByMerchantId(event.getMerchantId()).isPresent()) {
            log.info("Merchant already exists, skipping: merchantId={}", event.getMerchantId());
            return;
        }

        MerchantProfile profile = new MerchantProfile();
        profile.setMerchantId(event.getMerchantId());
        profile.setName(event.getMerchantName());
        profile.setUsername(event.getMerchantId()); // default username = merchantId
        profile.setPassword("123456");              // default password, merchant should change
        profile.setAddress(event.getMerchantAddress());
        profile.setPhone(event.getMerchantPhoneNum());
        profile.setContact(event.getMerchantContactPerson());
        profile.setCompany(event.getMerchantBank());

        try {
            profile.setJoinedOn(LocalDateTime.parse(event.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME));
        } catch (Exception ex) {
            profile.setJoinedOn(LocalDateTime.now());
        }

        profileRepository.save(profile);
        log.info("Inserted merchant into rta_user: merchantId={}", event.getMerchantId());
    }
}
