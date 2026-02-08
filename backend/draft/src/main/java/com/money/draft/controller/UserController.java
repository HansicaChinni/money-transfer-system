
package com.money.draft.controller;

import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.MeTransferRequest;
import com.money.draft.service.TransferService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
public class UserController {

    private final AppUserRepository userRepo;
    private final TransferService transferService;

    public UserController(AppUserRepository userRepo, TransferService transferService) {
        this.userRepo = userRepo;
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(Authentication auth, @RequestBody MeTransferRequest req) {
        var user = userRepo.findByUsername(auth.getName()).orElseThrow();

        // ✅ Server enforces fromAccountId and generates idempotencyKey
        var resp = transferService.transferForUser(
                user.getAccountId(),
                req.getToAccountId(),
                req.getAmount()
        );

        return ResponseEntity.ok(resp);
    }
}
