
package com.money.draft.service.impl;

import com.money.draft.domain.audit.AuditLog;
import com.money.draft.domain.audit.AuditLogRepository;
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
    private final AuditLogRepository auditLogRepo;

    public AccountServiceImpl(AccountRepository accountRepo,
                              TransactionLogRepository txRepo,
                              AppUserRepository userRepo,
                              PasswordEncoder passwordEncoder,
                              AuditLogRepository auditLogRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditLogRepo = auditLogRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(Long id) {
        Account a = accountRepo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        return new AccountResponse(a.getId(),a.getAccountNumber(), a.getHolderName(), a.getBalance(), a.getStatus().name());
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

        return logs.stream()
                .map(tx -> new TransactionLogResponse(
                        tx.getId(),
                        tx.getFromAccountId(),
                        tx.getToAccountId(),
                        tx.getAmount(),
                        tx.getStatus().name(),
                        tx.getFailureReason(),
                        tx.getIdempotencyKey(),
                        toLocalDateTime(tx.getCreatedOn()) // Instant -> LocalDateTime (UTC)
                ))
                .collect(Collectors.toList());
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));

        // Check old password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IncorrectPasswordException();
        }

        // Update
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        auditLogRepo.save(new AuditLog(
                "PASSWORD_CHANGE", "AppUser", userId, user.getUsername(),
                null, "changed"
        ));
    }
}
