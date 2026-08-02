package com.fpt.payments.event;

import java.util.List;
import java.util.UUID;

public record FraudAssessmentEvent(
        UUID transactionId,
        String correlationId,
        String decision,  // APPROVED | REJECTED
        int riskScore,
        List<String> reasons,
        String timestamp
) {}
