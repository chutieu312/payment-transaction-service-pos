package com.fpt.payments.enums;

import java.util.Map;
import java.util.Set;

public enum TransactionStatus {
    PENDING_FRAUD_CHECK,
    PROCESSING,
    COMPLETED,
    FAILED,
    FRAUD_REJECTED,
    REVERSED;

    private static final Map<TransactionStatus, Set<TransactionStatus>> VALID_TRANSITIONS = Map.of(
            PENDING_FRAUD_CHECK, Set.of(PROCESSING, FRAUD_REJECTED),
            PROCESSING, Set.of(COMPLETED, FAILED),
            COMPLETED, Set.of(REVERSED),
            FAILED, Set.of(),
            FRAUD_REJECTED, Set.of(),
            REVERSED, Set.of()
    );

    public boolean canTransitionTo(TransactionStatus next) {
        return VALID_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
