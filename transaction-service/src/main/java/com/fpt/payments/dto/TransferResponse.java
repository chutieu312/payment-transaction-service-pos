package com.fpt.payments.dto;

import com.fpt.payments.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        String idempotencyKey,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
