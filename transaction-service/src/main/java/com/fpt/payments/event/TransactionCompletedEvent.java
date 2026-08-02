package com.fpt.payments.event;

import com.fpt.payments.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionCompletedEvent(
        UUID transactionId,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        TransactionStatus finalStatus,
        Instant timestamp
) {}
