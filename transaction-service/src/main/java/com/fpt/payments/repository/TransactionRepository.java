package com.fpt.payments.repository;

import com.fpt.payments.entity.Transaction;
import com.fpt.payments.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(UUID fromId, UUID toId);
    List<Transaction> findByStatus(TransactionStatus status);
}
