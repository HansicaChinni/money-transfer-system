
package com.money.draft.controller;

import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // GET /api/v1/accounts/{id}
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    // GET /api/v1/accounts/{id}/balance
    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    // GET /api/v1/accounts/{id}/transactions
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionLogResponse>> getTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.getTransactions(id));
    }
}
