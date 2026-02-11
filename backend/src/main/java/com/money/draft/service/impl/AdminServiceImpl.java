
package com.money.draft.service.impl;

import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;

    public AdminServiceImpl(AccountRepository accountRepo, TransactionLogRepository txRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAccountView> getAllAccounts() {
        return accountRepo.findAll().stream()
                .map(a -> new AdminAccountView(
                        a.getId(),
                        a.getBalance(),
                        a.getStatus().name(),
                        toLocalDateTime(a.getLastUpdated()) // works for Instant or LocalDateTime
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getAllTransactions() {
        return txRepo.findAll().stream()
                .map(tx -> new TransactionLogResponse(
                        tx.getId(),
                        tx.getFromAccountId(),
                        tx.getToAccountId(),
                        tx.getAmount(),
                        tx.getStatus().name(),
                        tx.getFailureReason(),
                        tx.getIdempotencyKey(),
                        toLocalDateTime(tx.getCreatedOn()) // typically Instant -> LocalDateTime (UTC)
                ))
                .collect(Collectors.toList());
    }

    // --- Helpers ---

    // Convert Instant -> LocalDateTime (UTC)
    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    // Pass-through when already LocalDateTime
    private static LocalDateTime toLocalDateTime(LocalDateTime ldt) {
        return ldt;
    }
}
