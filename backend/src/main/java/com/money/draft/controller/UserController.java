
package com.money.draft.controller;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.ChangePasswordRequest;
import com.money.draft.dto.MeTransferRequest;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.dto.TransferResponse;
import com.money.draft.service.AccountService;
import com.money.draft.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final AppUserRepository appUserRepository;

    public UserController(AppUserRepository userRepo, AccountService accountService, TransferService transferService, AppUserRepository appUserRepository) {
        this.userRepo = userRepo;
        this.accountService = accountService;
        this.transferService = transferService;
        this.appUserRepository = appUserRepository;
    }

    @Operation(summary = "Initiate a transfer from my account")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(Authentication auth, @Valid @RequestBody MeTransferRequest req) {
        var user = userRepo.findByUsername(auth.getName()).orElseThrow();
        var resp = transferService.transferForUser(user.getAccountId(), req.toAccountId(), req.amount());
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Get my current balance")
    @GetMapping("/balance")
    public ResponseEntity<?> balance(Authentication auth) {
        var user = userRepo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(accountService.getAccount(user.getAccountId()));
    }

    @Operation(summary = "Get my transaction history (DESC)")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionLogResponse>> transactions(Authentication auth) {
        var user = userRepo.findByUsername(auth.getName()).orElseThrow();
        return ResponseEntity.ok(accountService.getTransactions(user.getAccountId()));
    }
    @PostMapping("/password")
        public ResponseEntity<?> changePassword(
                @Valid @RequestBody ChangePasswordRequest req,
                Authentication auth
        ) {
            if (auth == null || !auth.isAuthenticated()) {
                // let your 401 handler deal with this if you prefer
                throw new com.money.draft.exception.ValidationException("Not authenticated");
            }

            String username = auth.getName(); // set by JwtAuthFilter
            AppUser user = appUserRepository.findByUsername(username)
                    .orElseThrow(() -> new com.money.draft.exception.AccountNotFoundException(-1L));

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



