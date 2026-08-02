package com.fpt.payments.service;

import com.fpt.payments.dto.TransferRequest;
import com.fpt.payments.entity.Account;
import com.fpt.payments.entity.Transaction;
import com.fpt.payments.enums.TransactionStatus;
import com.fpt.payments.event.FraudAssessmentEvent;
import com.fpt.payments.exception.InsufficientFundsException;
import com.fpt.payments.exception.InvalidStatusTransitionException;
import com.fpt.payments.exception.NotFoundException;
import com.fpt.payments.kafka.TransactionEventProducer;
import com.fpt.payments.repository.AccountRepository;
import com.fpt.payments.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock AccountRepository accountRepository;
    @Mock TransactionEventProducer producer;
    @Mock AuditService auditService;
    @InjectMocks TransactionService transactionService;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = Account.builder()
                .id(UUID.randomUUID()).accountNumber("ACC-001")
                .balance(new BigDecimal("10000.00")).currency("USD").status("ACTIVE").build();
        toAccount = Account.builder()
                .id(UUID.randomUUID()).accountNumber("ACC-002")
                .balance(new BigDecimal("5000.00")).currency("USD").status("ACTIVE").build();
    }

    @Test
    void initiateTransfer_success_returnsPendingFraudCheck() {
        var req = new TransferRequest(toAccount.getId(), new BigDecimal("500"), "USD", "key-001", "test");
        when(transactionRepository.findByIdempotencyKey("key-001")).thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        var response = transactionService.initiateTransfer(fromAccount.getId(), req);

        assertThat(response.status()).isEqualTo(TransactionStatus.PENDING_FRAUD_CHECK);
        verify(transactionRepository).save(any());
    }

    @Test
    void initiateTransfer_duplicateIdempotencyKey_returnsCachedResult() {
        var existing = Transaction.builder()
                .id(UUID.randomUUID()).fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId()).amount(new BigDecimal("500")).currency("USD")
                .status(TransactionStatus.COMPLETED).idempotencyKey("key-dup").build();
        when(transactionRepository.findByIdempotencyKey("key-dup")).thenReturn(Optional.of(existing));

        var req = new TransferRequest(toAccount.getId(), new BigDecimal("500"), "USD", "key-dup", null);
        var response = transactionService.initiateTransfer(fromAccount.getId(), req);

        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void initiateTransfer_insufficientFunds_throwsException() {
        var req = new TransferRequest(toAccount.getId(), new BigDecimal("99999"), "USD", "key-002", null);
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(accountRepository.findById(fromAccount.getId())).thenReturn(Optional.of(fromAccount));

        assertThatThrownBy(() -> transactionService.initiateTransfer(fromAccount.getId(), req))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void processFraudAssessment_approved_completesTransfer() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID()).fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId()).amount(new BigDecimal("500"))
                .currency("USD").status(TransactionStatus.PENDING_FRAUD_CHECK).build();

        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(accountRepository.findByIdForUpdate(fromAccount.getId())).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByIdForUpdate(toAccount.getId())).thenReturn(Optional.of(toAccount));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var event = new FraudAssessmentEvent(tx.getId(), "corr-1", "APPROVED", 20,
                List.of(), Instant.now().toString());
        transactionService.processFraudAssessment(event);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(fromAccount.getBalance()).isEqualByComparingTo("9500.00");
        assertThat(toAccount.getBalance()).isEqualByComparingTo("5500.00");
    }

    @Test
    void processFraudAssessment_rejected_setsFraudRejected() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID()).fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId()).amount(new BigDecimal("500"))
                .currency("USD").status(TransactionStatus.PENDING_FRAUD_CHECK).build();

        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var event = new FraudAssessmentEvent(tx.getId(), "corr-2", "REJECTED", 85,
                List.of("AMOUNT_THRESHOLD_EXCEEDED"), Instant.now().toString());
        transactionService.processFraudAssessment(event);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FRAUD_REJECTED);
    }

    @Test
    void reverseTransaction_invalidTransition_throwsException() {
        Transaction tx = Transaction.builder()
                .id(UUID.randomUUID()).status(TransactionStatus.PENDING_FRAUD_CHECK).build();
        when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> transactionService.reverseTransaction(tx.getId()))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void transactionStatus_stateTransitions_validGraphEnforced() {
        assertThat(TransactionStatus.PENDING_FRAUD_CHECK.canTransitionTo(TransactionStatus.PROCESSING)).isTrue();
        assertThat(TransactionStatus.PENDING_FRAUD_CHECK.canTransitionTo(TransactionStatus.REVERSED)).isFalse();
        assertThat(TransactionStatus.COMPLETED.canTransitionTo(TransactionStatus.REVERSED)).isTrue();
        assertThat(TransactionStatus.REVERSED.canTransitionTo(TransactionStatus.COMPLETED)).isFalse();
        assertThat(TransactionStatus.FRAUD_REJECTED.canTransitionTo(TransactionStatus.PROCESSING)).isFalse();
    }
}
