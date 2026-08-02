package com.fpt.payments.kafka;

import com.fpt.payments.event.FraudAssessmentEvent;
import com.fpt.payments.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAssessmentConsumer {

    private final TransactionService transactionService;

    @KafkaListener(topics = "fraud.assessment", groupId = "transaction-service")
    public void consume(FraudAssessmentEvent event) {
        log.info("Received fraud.assessment for tx={} decision={} score={}",
                event.transactionId(), event.decision(), event.riskScore());
        transactionService.processFraudAssessment(event);
    }
}
