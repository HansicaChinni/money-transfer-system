package com.money.draft.controller;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.ChangePasswordRequest;
import com.money.draft.dto.MeTransferRequest;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.dto.TransferResponse;
import com.money.draft.exception.ValidationException;
import com.money.draft.service.AccountService;
import com.money.draft.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "User", description = "User portal: balance, transactions, transfers")
@RestController
@RequestMapping("/me")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final AppUserRepository userRepo;
    private final AccountService accountService;
    private final TransferService transferService;

    public UserController(AppUserRepository userRepo, AccountService accountService, TransferService transferService) {
        this.userRepo = userRepo;
        this.accountService = accountService;
        this.transferService = transferService;
    }

    @Operation(summary = "Initiate a transfer from my account")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody MeTransferRequest req) {
        AppUser user = getCurrentUser();
        var resp = transferService.transferForUser(user.getAccountId(), req.toAccountId(), req.amount());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get my current balance")
    @GetMapping("/balance")
    public ResponseEntity<?> balance() {
        AppUser user = getCurrentUser();
        return ResponseEntity.ok(accountService.getAccount(user.getAccountId()));
    }

    @Operation(summary = "Get my transaction history (DESC)")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionLogResponse>> transactions() {
        AppUser user = getCurrentUser();
        return ResponseEntity.ok(accountService.getTransactions(user.getAccountId()));
    }

    @PostMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        AppUser user = getCurrentUser();
        accountService.changePassword(
                user.getId(),
                req.currentPassword(),
                req.newPassword()
        );
        return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully. Please login again."
        ));
    }

    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ValidationException("Not authenticated");
        }
        String username = auth.getName();
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ValidationException("User not found"));
    }
}
