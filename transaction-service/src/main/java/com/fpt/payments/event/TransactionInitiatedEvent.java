package com.fpt.payments.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionInitiatedEvent(
        UUID transactionId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String correlationId,
        Instant timestamp
) {}
