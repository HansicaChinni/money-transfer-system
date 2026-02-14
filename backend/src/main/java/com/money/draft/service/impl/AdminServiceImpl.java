
package com.money.draft.service.impl;


import com.money.draft.domain.entity.Account;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminAccountDetailResponse;
import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.AdminCreateAccountRequest;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.exception.ValidationException;
import com.money.draft.service.AdminService;
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
public class AdminServiceImpl implements AdminService {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(AccountRepository accountRepo,
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
    public List<AdminAccountView> getAllAccounts() {
        return accountRepo.findAll().stream()
                .map(a -> new AdminAccountView(
                        a.getId(),
                        a.getAccountNumber(),
                        a.getBalance(),
                        a.getStatus().name(),
                        toLocalDateTime(a.getLastUpdated()) // works for Instant or LocalDateTime
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
                        toLocalDateTime(tx.getCreatedOn()) // typically Instant -> LocalDateTime (UTC)
                ))
                .toList();
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

    @Override
    @Transactional
    public AdminAccountDetailResponse createAccount(AdminCreateAccountRequest req) {
        // 1. Prevent duplicate usernames
        if (userRepo.findByUsername(req.username()).isPresent()) {
            throw new com.money.draft.exception.BusinessException("DUPLICATE_USER", "Username already exists");
        }

        // 2. Create and Save Account (image_9cc5f4.png fields)
        com.money.draft.domain.entity.Account account = new com.money.draft.domain.entity.Account();
        account.setHolderName(req.holderName());
        if (req.initialBalance().compareTo(new BigDecimal("1000")) < 0) {
            throw new ValidationException(
                    "Initial deposit must be at least ₹1000"
            );
        }

        account.setBalance(req.initialBalance());
        account.setStatus(com.money.draft.domain.enums.AccountStatus.ACTIVE);

        Account saved = accountRepo.save(account);

        // Generate formatted account number
        saved.setAccountNumber(
                Account.generateAccountNumber(saved.getId())
        );

        accountRepo.save(saved);

        // 3. Create and Save User (image_9cc8d6.png fields)
        com.money.draft.domain.entity.AppUser user = new com.money.draft.domain.entity.AppUser();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(com.money.draft.domain.enums.Role.USER);
        user.setAccountId(saved.getId());

        userRepo.save(user);

        return mapToAdminDetailResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAccountDetailResponse getAccountDetails(Long id) {
        com.money.draft.domain.entity.Account a = accountRepo.findById(id)
                .orElseThrow(() -> new com.money.draft.exception.AccountNotFoundException(id));
        return mapToAdminDetailResponse(a);
    }

    @Override
    @Transactional
    public AdminAccountDetailResponse updateAccountStatus(Long id, com.money.draft.domain.enums.AccountStatus status) {
        com.money.draft.domain.entity.Account a = accountRepo.findById(id)
                .orElseThrow(() -> new com.money.draft.exception.AccountNotFoundException(id));

        a.setStatus(status);
        return mapToAdminDetailResponse(accountRepo.save(a));
    }

    // Mapper for the Admin-specific detail view
    private AdminAccountDetailResponse mapToAdminDetailResponse(com.money.draft.domain.entity.Account a) {
        return new AdminAccountDetailResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getHolderName(),
                a.getBalance(),
                a.getStatus().name(),
                a.getVersion(),
                toLocalDateTime(a.getLastUpdated())
        );
    }
}
