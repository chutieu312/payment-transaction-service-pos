package com.fpt.payments.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID toAccountId,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount,
        @NotBlank @Size(max = 3) String currency,
        @NotBlank String idempotencyKey,
        String description
) {}
