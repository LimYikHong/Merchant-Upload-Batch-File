package rta.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import rta.entity.ExternalMerchantInfo;
import rta.kafka.MerchantCreatedEvent;
import rta.repository.ExternalMerchantInfoRepository;

@Service
public class ExternalMerchantInfoService {

    private final ExternalMerchantInfoRepository repository;

    public ExternalMerchantInfoService(ExternalMerchantInfoRepository repository) {
        this.repository = repository;
    }

    public ExternalMerchantInfo upsertFromEvent(MerchantCreatedEvent e) {
        ExternalMerchantInfo info = repository.findByMerchantId(e.getMerchantId()).orElse(new ExternalMerchantInfo());
        info.setMerchantId(e.getMerchantId());
        info.setMerchantName(e.getMerchantName());
        info.setMerchantBank(e.getMerchantBank());
        info.setMerchantCode(e.getMerchantCode());
        info.setMerchantPhoneNum(e.getMerchantPhoneNum());
        info.setMerchantAddress(e.getMerchantAddress());
        info.setMerchantContactPerson(e.getMerchantContactPerson());
        info.setMerchantStatus(e.getMerchantStatus());
        info.setCreatedBy(e.getCreatedBy());

        try {
            info.setCreatedAt(LocalDateTime.parse(e.getCreatedAt(), DateTimeFormatter.ISO_DATE_TIME));
        } catch (Exception ex) {
            info.setCreatedAt(LocalDateTime.now());
        }

        info.setMerchantAccNum(e.getMerchantAccNum());
        info.setMerchantAccName(e.getMerchantAccName());
        info.setTransactionCurrency(e.getTransactionCurrency());
        info.setSettlementCurrency(e.getSettlementCurrency());

        return repository.save(info);
    }
}
