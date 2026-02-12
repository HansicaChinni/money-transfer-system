package com.money.draft.controller;

import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.dto.*;
import com.money.draft.service.AccountService;
import com.money.draft.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "Admin portal: Account management and transaction oversight")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {
    private final AdminService adminService;
    private AccountService accountService;

    public AdminController(AdminService adminService, AccountService accountService) {
        this.adminService = adminService;
        this.accountService = accountService;
    }


    @Operation(summary = "List all accounts (privacy-safe: no holder names)")
    @GetMapping("/accounts")
    public ResponseEntity<List<AdminAccountView>> allAccountsNoNames() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @Operation(summary = "Search for a specific account by ID (Full Details)")
    @GetMapping("/accounts/{id}")
// Change AccountResponse -> AdminAccountDetailResponse
    public ResponseEntity<AdminAccountDetailResponse> getAccountDetails(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getAccountDetails(id));
    }

    @Operation(summary = "Get transaction history of given account id")
    @GetMapping("/transactions/{id}")
    public ResponseEntity<List<TransactionLogResponse>> getAccountTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getTransactions(id));
    }

    @Operation(summary = "Create a new account and associated user login")
    @PostMapping("/accounts")
// Change AccountResponse -> AdminAccountDetailResponse
    public ResponseEntity<AdminAccountDetailResponse> createAccount(@Valid @RequestBody AdminCreateAccountRequest req) {
        return ResponseEntity.ok(adminService.createAccount(req));
    }

    @Operation(summary = "Update the status of an account (ACTIVE, LOCKED, CLOSED)")
    @PatchMapping("/accounts/{id}/status")
    public ResponseEntity<AdminAccountDetailResponse> updateAccountStatus(
            @PathVariable Long id,
            @RequestParam AccountStatus status) {
        return ResponseEntity.ok(adminService.updateAccountStatus(id, status));
    }

    @Operation(summary = "List all transaction logs")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionLogResponse>> allTransactions() {
        return ResponseEntity.ok(adminService.getAllTransactions());
    }
}