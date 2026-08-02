package com.fpt.payments.service;

import com.fpt.payments.dto.TransferRequest;
import com.fpt.payments.dto.TransferResponse;
import com.fpt.payments.entity.Account;
import com.fpt.payments.entity.Transaction;
import com.fpt.payments.enums.TransactionStatus;
import com.fpt.payments.event.FraudAssessmentEvent;
import com.fpt.payments.event.TransactionCompletedEvent;
import com.fpt.payments.event.TransactionInitiatedEvent;
import com.fpt.payments.exception.InsufficientFundsException;
import com.fpt.payments.exception.InvalidStatusTransitionException;
import com.fpt.payments.exception.NotFoundException;
import com.fpt.payments.kafka.TransactionEventProducer;
import com.fpt.payments.repository.AccountRepository;
import com.fpt.payments.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionEventProducer producer;
    private final AuditService auditService;

    /**
     * Initiates a transfer. Returns immediately (202) after persisting and publishing to Kafka.
     * The fraud check happens asynchronously. Status starts as PENDING_FRAUD_CHECK.
     */
    @Transactional
    public TransferResponse initiateTransfer(UUID fromAccountId, TransferRequest req) {
        // Idempotency check — return existing result if key already used
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(req.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Idempotency key {} already used, returning cached result", req.idempotencyKey());
            return toResponse(existing.get());
        }

        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new NotFoundException("Source account not found"));
        if (!"ACTIVE".equals(fromAccount.getStatus())) {
            throw new IllegalStateException("Source account is not active");
        }
        if (fromAccount.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        Transaction tx = Transaction.builder()
                .fromAccountId(fromAccountId)
                .toAccountId(req.toAccountId())
                .amount(req.amount())
                .currency(req.currency())
                .status(TransactionStatus.PENDING_FRAUD_CHECK)
                .idempotencyKey(req.idempotencyKey())
                .description(req.description())
                .build();
        Transaction saved = transactionRepository.save(tx);
        auditService.log(saved.getId(), null, TransactionStatus.PENDING_FRAUD_CHECK, "SYSTEM");

        // Publish Kafka event AFTER successful DB write (outside @Transactional boundary)
        return toResponse(saved);
    }

    /**
     * Called after DB commit — publishes Kafka event to trigger fraud check.
     */
    public void publishFraudCheckEvent(Transaction tx) {
        var event = new TransactionInitiatedEvent(
                tx.getId(), tx.getFromAccountId(), tx.getToAccountId(),
                tx.getAmount(), tx.getCurrency(),
                UUID.randomUUID().toString(), Instant.now());
        producer.publishTransactionInitiated(event);
    }

    /**
     * Consumes fraud.assessment Kafka event. Executes the balance transfer if APPROVED.
     */
    @Transactional
    @CacheEvict(value = "balances", allEntries = true)
    public void processFraudAssessment(FraudAssessmentEvent event) {
        Transaction tx = transactionRepository.findById(event.transactionId())
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + event.transactionId()));

        if (!"APPROVED".equals(event.decision())) {
            updateStatus(tx, TransactionStatus.FRAUD_REJECTED);
            publishCompletedEvent(tx);
            return;
        }

        updateStatus(tx, TransactionStatus.PROCESSING);

        try {
            Account from = accountRepository.findByIdForUpdate(tx.getFromAccountId())
                    .orElseThrow(() -> new NotFoundException("Source account not found"));
            Account to = accountRepository.findByIdForUpdate(tx.getToAccountId())
                    .orElseThrow(() -> new NotFoundException("Destination account not found"));

            if (from.getBalance().compareTo(tx.getAmount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds at time of settlement");
            }

            from.setBalance(from.getBalance().subtract(tx.getAmount()));
            to.setBalance(to.getBalance().add(tx.getAmount()));
            accountRepository.save(from);
            accountRepository.save(to);

            updateStatus(tx, TransactionStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Transfer execution failed for tx {}: {}", tx.getId(), e.getMessage());
            updateStatus(tx, TransactionStatus.FAILED);
        }

        publishCompletedEvent(tx);
    }

    public TransferResponse getTransaction(UUID txId) {
        return toResponse(transactionRepository.findById(txId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + txId)));
    }

    public List<TransferResponse> getAccountTransactions(UUID accountId) {
        return transactionRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public TransferResponse reverseTransaction(UUID txId) {
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        if (!tx.getStatus().canTransitionTo(TransactionStatus.REVERSED)) {
            throw new InvalidStatusTransitionException(
                    tx.getStatus() + " cannot transition to REVERSED");
        }
        updateStatus(tx, TransactionStatus.REVERSED);
        return toResponse(tx);
    }

    private void updateStatus(Transaction tx, TransactionStatus newStatus) {
        TransactionStatus old = tx.getStatus();
        tx.setStatus(newStatus);
        transactionRepository.save(tx);
        auditService.log(tx.getId(), old, newStatus, "SYSTEM");
    }

    private void publishCompletedEvent(Transaction tx) {
        producer.publishTransactionCompleted(new TransactionCompletedEvent(
                tx.getId(), tx.getFromAccountId(), tx.getToAccountId(),
                tx.getAmount(), tx.getCurrency(), tx.getStatus(), Instant.now()));
    }

    private TransferResponse toResponse(Transaction tx) {
        return new TransferResponse(tx.getId(), tx.getFromAccountId(), tx.getToAccountId(),
                tx.getAmount(), tx.getCurrency(), tx.getStatus(), tx.getIdempotencyKey(),
                tx.getDescription(), tx.getCreatedAt(), tx.getUpdatedAt());
    }
}
