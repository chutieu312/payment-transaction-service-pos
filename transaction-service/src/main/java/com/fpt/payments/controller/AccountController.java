package com.fpt.payments.controller;

import com.fpt.payments.dto.AccountResponse;
import com.fpt.payments.dto.BalanceResponse;
import com.fpt.payments.dto.TransferResponse;
import com.fpt.payments.service.AccountService;
import com.fpt.payments.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;
    private final TransactionService transactionService;

    @GetMapping("/{id}")
    @Operation(summary = "Get account details")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get account balance (cached in Redis)")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Get transaction history for an account")
    public ResponseEntity<List<TransferResponse>> getTransactions(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getAccountTransactions(id));
    }
}
