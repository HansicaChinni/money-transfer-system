
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.service.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;

    public AccountServiceImpl(AccountRepository accountRepo, TransactionLogRepository txRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account a = accountRepo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return new AccountResponse(a.getId(), a.getHolderName(), a.getBalance(), String.valueOf(a.getStatus()));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long id) {
        Account a = accountRepo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return a.getBalance();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getTransactions(Long accountId) {
        // fetch all logs and filter in-memory; for real systems, add repository queries
        List<TransactionLog> logs = txRepo.findAll();
        return logs.stream()
                .filter(tx -> accountId.equals(tx.getFromAccountId()) || accountId.equals(tx.getToAccountId()))
                .sorted(Comparator.comparing(TransactionLog::getCreatedOn).reversed())
                .map(tx -> new TransactionLogResponse(
                        tx.getId(),
                        tx.getFromAccountId(),
                        tx.getToAccountId(),
                        tx.getAmount(),
                        String.valueOf(tx.getStatus()),
                        tx.getFailureReason(),
                        tx.getIdempotencyKey(),
                        tx.getCreatedOn()
                ))
                .toList();
    }
}
