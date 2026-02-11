
package com.money.draft.service.impl;

import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.service.AdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                        a.getLastUpdated()
                ))
                .toList();
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
                        tx.getCreatedOn()
                ))
                .toList();
    }
}
