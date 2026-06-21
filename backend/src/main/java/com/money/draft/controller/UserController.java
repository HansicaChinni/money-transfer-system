
package com.money.draft.controller;

import com.money.draft.domain.entity.Account;
import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.*;
import com.money.draft.exception.AccountNotFoundException;
import com.money.draft.exception.ValidationException;
import com.money.draft.service.AccountService;
import com.money.draft.service.RewardService;
import com.money.draft.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Tag(name = "User", description = "User portal: balance, transactions, transfers, rewards")
@RestController
@RequestMapping("/me")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final AppUserRepository userRepo;
    private final AccountRepository accountRepo;
    private final AccountService accountService;
    private final TransferService transferService;
    private final AppUserRepository appUserRepository;
    private final RewardService rewardService;

    public UserController(AppUserRepository userRepo, AccountRepository accountRepo, AccountService accountService,
                          TransferService transferService, AppUserRepository appUserRepository,
                          RewardService rewardService) {
        this.userRepo = userRepo;
        this.accountRepo = accountRepo;
        this.accountService = accountService;
        this.transferService = transferService;
        this.appUserRepository = appUserRepository;
        this.rewardService = rewardService;
    }

    private Authentication getAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private AppUser getCurrentUser() {
        return userRepo.findByUsername(getAuth().getName())
                .orElseThrow(() -> new AccountNotFoundException(-1L));
    }

    private Account getMyAccount() {
        AppUser user = getCurrentUser();
        if (user.getAccountId() == null) {
            throw new ValidationException("No account linked to this user");
        }
        return accountRepo.findById(user.getAccountId())
                .orElseThrow(() -> new AccountNotFoundException(user.getAccountId()));
    }

    @Operation(summary = "Export transactions as CSV")
    @GetMapping("/transactions/export")
    public ResponseEntity<org.springframework.core.io.Resource> exportTransactions() {
        Account account = getMyAccount();
        List<TransactionLogResponse> txs = accountService.getTransactions(account.getId());
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Date,Type,Counterparty,Amount,Points,Status\n");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (TransactionLogResponse tx : txs) {
            String type = tx.fromAccountId().equals(account.getId()) ? "Sent" : "Received";
            String counterparty = tx.fromAccountId().equals(account.getId()) ? tx.toHolderName() : tx.fromHolderName();
            String amount = (tx.fromAccountId().equals(account.getId()) ? "-" : "+") + tx.amount();
            String points = tx.rewardPointsEarned() != null ? "+" + tx.rewardPointsEarned()
                    : (tx.rewardPointsUsed() != null ? "-" + tx.rewardPointsUsed() : "");
            csv.append(tx.id()).append(",")
               .append(tx.createdOn() != null ? tx.createdOn().format(dtf) : "").append(",")
               .append(type).append(",")
               .append(counterparty).append(",")
               .append(amount).append(",")
               .append(points).append(",")
               .append(tx.status()).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(bytes.length)
                .body(resource);
    }

    @Operation(summary = "Initiate a transfer from my account")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody MeTransferRequest req) {
        Account fromAccount = getMyAccount();
        Account toAccount = accountRepo.findByAccountNumber(req.toAccountNumber())
                .orElseThrow(() -> new ValidationException("Recipient account not found: " + req.toAccountNumber()));
        var resp = transferService.transferForUser(fromAccount.getId(), toAccount.getId(), req.amount(), req.useRewardPoints());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get my current balance")
    @GetMapping("/balance")
    public ResponseEntity<?> balance() {
        Account account = getMyAccount();
        return ResponseEntity.ok(accountService.getAccount(account.getId()));
    }

    @Operation(summary = "Get my transaction history (DESC)")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionLogResponse>> transactions() {
        Account account = getMyAccount();
        return ResponseEntity.ok(accountService.getTransactions(account.getId()));
    }

    @Operation(summary = "Get transaction detail with reward info")
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDetailResponse> transactionDetail(@PathVariable Long id) {
        Account account = getMyAccount();
        return ResponseEntity.ok(rewardService.getTransactionDetail(account.getId(), id));
    }

    @Operation(summary = "Get my reward points summary")
    @GetMapping("/rewards/summary")
    public ResponseEntity<RewardSummaryResponse> rewardSummary() {
        Account account = getMyAccount();
        return ResponseEntity.ok(rewardService.getRewardSummary(account.getId()));
    }

    @Operation(summary = "Get my reward transaction history")
    @GetMapping("/rewards/transactions")
    public ResponseEntity<List<RewardTransactionResponse>> rewardTransactions() {
        Account account = getMyAccount();
        return ResponseEntity.ok(rewardService.getRewardTransactions(account.getId()));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        Authentication auth = getAuth();
        if (auth == null || !auth.isAuthenticated()) {
            throw new com.money.draft.exception.ValidationException("Not authenticated");
        }

        String username = auth.getName();
        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new AccountNotFoundException(-1L));

        accountService.changePassword(
                user.getId(),
                req.currentPassword(),
                req.newPassword()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully. Please login again."
        ));
    }
}
