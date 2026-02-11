
package com.money.draft.controller;

import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.TransactionLogResponse;
import com.money.draft.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "Admin portal: all accounts (no names) and all transactions")
@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {

    private final AdminService adminService;
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Operation(summary = "List all accounts (no holder names)")
    @GetMapping("/accounts")
    public ResponseEntity<List<AdminAccountView>> allAccountsNoNames() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @Operation(summary = "List all transaction logs")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionLogResponse>> allTransactions() {
        return ResponseEntity.ok(adminService.getAllTransactions());
    }
}
