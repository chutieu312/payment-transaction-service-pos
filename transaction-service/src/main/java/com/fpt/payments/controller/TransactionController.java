package com.fpt.payments.controller;

import com.fpt.payments.dto.TransferRequest;
import com.fpt.payments.dto.TransferResponse;
import com.fpt.payments.entity.Transaction;
import com.fpt.payments.exception.NotFoundException;
import com.fpt.payments.repository.TransactionRepository;
import com.fpt.payments.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfers", description = "Fund transfer endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @PostMapping("/{fromAccountId}")
    @Operation(summary = "Initiate a fund transfer (async — returns 202 immediately)")
    public ResponseEntity<TransferResponse> initiateTransfer(
            @PathVariable UUID fromAccountId,
            @Valid @RequestBody TransferRequest req) {
        TransferResponse response = transactionService.initiateTransfer(fromAccountId, req);
        // Publish Kafka event after DB commit
        transactionRepository.findById(response.id()).ifPresent(
                transactionService::publishFraudCheckEvent);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer status (poll this after initiating)")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping
    @Operation(summary = "List all transfers (BANK_ADMIN only)")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<List<TransferResponse>> listAll() {
        List<TransferResponse> all = transactionRepository.findAll().stream()
                .map(tx -> transactionService.getTransaction(tx.getId()))
                .toList();
        return ResponseEntity.ok(all);
    }

    @PostMapping("/{id}/reverse")
    @Operation(summary = "Reverse a completed transfer (BANK_ADMIN only)")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<TransferResponse> reverseTransfer(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.reverseTransaction(id));
    }
}
