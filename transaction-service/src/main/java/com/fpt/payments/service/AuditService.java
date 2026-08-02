package com.fpt.payments.service;

import com.fpt.payments.enums.TransactionStatus;
import com.fpt.payments.mongo.TransactionEvent;
import com.fpt.payments.repository.TransactionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final TransactionEventRepository eventRepository;

    public void log(UUID transactionId, TransactionStatus from, TransactionStatus to, String actor) {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(transactionId)
                .fromStatus(from != null ? from.name() : "NEW")
                .toStatus(to.name())
                .changedBy(actor)
                .timestamp(Instant.now())
                .build();
        eventRepository.save(event);
        log.info("Audit: tx={} {} -> {} by {}", transactionId, event.getFromStatus(), to, actor);
    }
}
