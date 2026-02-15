package rta.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import rta.service.ExternalMerchantInfoService;

@Component
public class MerchantCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(MerchantCreatedConsumer.class);
    private final ExternalMerchantInfoService service;

    public MerchantCreatedConsumer(ExternalMerchantInfoService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${rta.kafka.topic.merchant-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(@Payload MerchantCreatedEvent event) {
        log.info("Received merchant-created event: merchantId={}", event.getMerchantId());
        service.upsertFromEvent(event);
        log.info("Persisted merchant info: merchantId={}", event.getMerchantId());
    }
}
