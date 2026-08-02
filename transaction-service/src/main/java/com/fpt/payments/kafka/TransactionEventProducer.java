package com.fpt.payments.kafka;

import com.fpt.payments.event.TransactionCompletedEvent;
import com.fpt.payments.event.TransactionInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionInitiated(TransactionInitiatedEvent event) {
        log.info("Publishing transaction.initiated for tx={}", event.transactionId());
        kafkaTemplate.send("transaction.initiated", event.transactionId().toString(), event);
    }

    public void publishTransactionCompleted(TransactionCompletedEvent event) {
        log.info("Publishing transaction.completed for tx={} status={}", event.transactionId(), event.finalStatus());
        kafkaTemplate.send("transaction.completed", event.transactionId().toString(), event);
    }
}
