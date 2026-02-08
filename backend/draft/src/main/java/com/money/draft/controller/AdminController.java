package com.money.draft.controller;


import com.money.draft.domain.repository.AccountRepository;
import com.money.draft.domain.repository.TransactionLogRepository;
import com.money.draft.dto.AdminAccountView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AccountRepository accountRepo;
    private final TransactionLogRepository txRepo;

    public AdminController(AccountRepository accountRepo, TransactionLogRepository txRepo) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
    }

    // All accounts WITHOUT names

    @GetMapping("/accounts")
    public ResponseEntity<?> allAccountsNoNames() {
        var list = accountRepo.findAll().stream()
                .map(a -> new AdminAccountView(
                        a.getId(),
                        a.getBalance(),
                        a.getStatus().name(),
                        a.getLastUpdated()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }


    @GetMapping("/transactions")
    public ResponseEntity<?> allTransactions() {
        return ResponseEntity.ok(txRepo.findAll());
    }
}
