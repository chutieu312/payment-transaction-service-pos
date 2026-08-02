package com.fpt.payments.service;

import com.fpt.payments.dto.AccountResponse;
import com.fpt.payments.dto.BalanceResponse;
import com.fpt.payments.entity.Account;
import com.fpt.payments.exception.NotFoundException;
import com.fpt.payments.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Cacheable(value = "balances", key = "#accountId")
    public BalanceResponse getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        return new BalanceResponse(account.getId(), account.getAccountNumber(),
                account.getBalance(), account.getCurrency());
    }

    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));
        return toResponse(account);
    }

    public List<AccountResponse> getAccountsByOwner(UUID ownerId) {
        return accountRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AccountResponse toResponse(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getOwnerId(),
                a.getBalance(), a.getCurrency(), a.getStatus(), a.getCreatedAt());
    }
}
