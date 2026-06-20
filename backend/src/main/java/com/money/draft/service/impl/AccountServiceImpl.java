
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.exception.IncorrectPasswordException;
import com.money.draft.service.AccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepo,
                              TransactionLogRepository txRepo,
                              AppUserRepository userRepo,
                              PasswordEncoder passwordEncoder) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account a = accountRepo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getHolderName(), a.getBalance(), a.getStatus().name(), a.getRewardPoints());
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
        List<TransactionLog> logs =
                txRepo.findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(accountId, accountId);

        java.util.Map<Long, Account> accountCache = loadAccounts(logs);
        return logs.stream()
                .map(tx -> new TransactionLogResponse(
                        tx.getId(),
                        tx.getFromAccountId(),
                        tx.getToAccountId(),
                        resolveAccountNumber(tx.getFromAccountId(), accountCache),
                        resolveAccountNumber(tx.getToAccountId(), accountCache),
                        resolveHolderName(tx.getFromAccountId(), accountCache),
                        resolveHolderName(tx.getToAccountId(), accountCache),
                        tx.getAmount(),
                        tx.getStatus().name(),
                        tx.getFailureReason(),
                        tx.getIdempotencyKey(),
                        toLocalDateTime(tx.getCreatedOn()),
                        tx.getRewardPointsEarned(),
                        tx.getRewardPointsUsed()
                ))
                .collect(Collectors.toList());
    }

    private java.util.Map<Long, Account> loadAccounts(List<TransactionLog> txs) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (TransactionLog tx : txs) {
            ids.add(tx.getFromAccountId());
            ids.add(tx.getToAccountId());
        }
        return accountRepo.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Account::getId, a -> a));
    }

    private static String resolveAccountNumber(Long accountId, java.util.Map<Long, Account> cache) {
        Account a = cache.get(accountId);
        return a != null ? a.getAccountNumber() : "#" + accountId;
    }

    private static String resolveHolderName(Long accountId, java.util.Map<Long, Account> cache) {
        Account a = cache.get(accountId);
        return a != null ? a.getHolderName() : "Unknown";
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IncorrectPasswordException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }
}
