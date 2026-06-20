
package com.money.draft.service.impl;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.enums.RewardTransactionType;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.domain.repository.RewardTransactionRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminAccountDetailResponse;
import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.AdminCreateAccountRequest;
import com.money.draft.dto.TransactionDetailResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RewardTransactionRepository rewardRepo;

    public AdminServiceImpl(AccountRepository accountRepo,
                            TransactionLogRepository txRepo,
                            AppUserRepository userRepo,
                            PasswordEncoder passwordEncoder,
                            RewardTransactionRepository rewardRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.rewardRepo = rewardRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminAccountView> getAllAccounts() {
        return accountRepo.findAll().stream()
                .map(a -> new AdminAccountView(
                        a.getId(),
                        a.getAccountNumber(),
                        a.getHolderName(),
                        a.getBalance(),
                        a.getStatus().name(),
                        toLocalDateTime(a.getLastUpdated())
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionLogResponse> getAllTransactions() {
        List<TransactionLog> txs = txRepo.findAll();
        Map<Long, Account> accountCache = loadAccounts(txs);
        return txs.stream()
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
                .toList();
    }

    private Map<Long, Account> loadAccounts(List<TransactionLog> txs) {
        Set<Long> ids = new HashSet<>();
        for (TransactionLog tx : txs) {
            ids.add(tx.getFromAccountId());
            ids.add(tx.getToAccountId());
        }
        return accountRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));
    }

    private static String resolveAccountNumber(Long accountId, Map<Long, Account> cache) {
        Account a = cache.get(accountId);
        return a != null ? a.getAccountNumber() : "#" + accountId;
    }

    private static String resolveHolderName(Long accountId, Map<Long, Account> cache) {
        Account a = cache.get(accountId);
        return a != null ? a.getHolderName() : "Unknown";
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    private static LocalDateTime toLocalDateTime(LocalDateTime ldt) {
        return ldt;
    }

    @Override
    @Transactional
    public AdminAccountDetailResponse createAccount(AdminCreateAccountRequest req) {
        if (userRepo.findByUsername(req.username()).isPresent()) {
            throw new com.money.draft.exception.BusinessException("DUPLICATE_USER", "Username already exists");
        }

        Account account = new Account();
        account.setHolderName(req.holderName());
        if (req.initialBalance().compareTo(new BigDecimal("1000")) < 0) {
            throw new ValidationException("Initial deposit must be at least \u20B91000");
        }

        account.setBalance(req.initialBalance());
        account.setStatus(com.money.draft.domain.enums.AccountStatus.ACTIVE);

        Account saved = accountRepo.save(account);

        saved.setAccountNumber(Account.generateAccountNumber(saved.getId()));
        accountRepo.save(saved);

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
        Account a = accountRepo.findById(id)
                .orElseThrow(() -> new com.money.draft.exception.AccountNotFoundException(id));
        return mapToAdminDetailResponse(a);
    }

    @Override
    @Transactional
    public AdminAccountDetailResponse updateAccountStatus(Long id, com.money.draft.domain.enums.AccountStatus status) {
        Account a = accountRepo.findById(id)
                .orElseThrow(() -> new com.money.draft.exception.AccountNotFoundException(id));

        a.setStatus(status);
        return mapToAdminDetailResponse(accountRepo.save(a));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionDetail(Long transactionId) {
        TransactionLog tx = txRepo.findById(transactionId)
                .orElseThrow(() -> new com.money.draft.exception.AccountNotFoundException(transactionId));

        Map<Long, Account> accountCache = new java.util.HashMap<>();
        accountRepo.findById(tx.getFromAccountId()).ifPresent(a -> accountCache.put(a.getId(), a));
        accountRepo.findById(tx.getToAccountId()).ifPresent(a -> accountCache.put(a.getId(), a));

        return new TransactionDetailResponse(
                tx.getId(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                resolveAccountNumber(tx.getFromAccountId(), accountCache),
                resolveAccountNumber(tx.getToAccountId(), accountCache),
                tx.getAmount(),
                tx.getStatus().name(),
                tx.getFailureReason(),
                tx.getIdempotencyKey(),
                toLocalDateTime(tx.getCreatedOn()),
                tx.getRewardPointsEarned(),
                tx.getRewardPointsUsed()
        );
    }

    private AdminAccountDetailResponse mapToAdminDetailResponse(Account a) {
        int totalRedeemed = rewardRepo.sumPointsByAccountIdAndType(a.getId(), RewardTransactionType.REDEEMED);
        return new AdminAccountDetailResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getHolderName(),
                a.getBalance(),
                a.getStatus().name(),
                a.getVersion(),
                toLocalDateTime(a.getLastUpdated()),
                a.getRewardPoints(),
                totalRedeemed
        );
    }
}
