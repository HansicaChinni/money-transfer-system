package com.money.draft.controller;

import com.money.draft.domain.entity.AppUser;
import com.money.draft.domain.repository.AppUserRepository;
import com.money.draft.dto.*;
import com.money.draft.exception.ValidationException;
import com.money.draft.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/rewards")
public class UserRewardController {

    private final RewardService rewardService;
    private final AppUserRepository userRepo;

    public UserRewardController(RewardService rewardService, AppUserRepository userRepo) {
        this.rewardService = rewardService;
        this.userRepo = userRepo;
    }

    @GetMapping("/summary")
    public ResponseEntity<RewardSummaryResponse> getSummary() {
        return ResponseEntity.ok(rewardService.getRewardSummary(getCurrentAccountId()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<RewardTransactionResponse>> getHistory() {
        return ResponseEntity.ok(rewardService.getRewardHistory(getCurrentAccountId()));
    }

    @GetMapping("/store")
    public ResponseEntity<List<RewardItemResponse>> getStore() {
        return ResponseEntity.ok(rewardService.getAvailableItems());
    }

    @PostMapping("/redeem")
    public ResponseEntity<RedemptionResponse> redeem(@Valid @RequestBody RedeemRequest req) {
        return ResponseEntity.ok(rewardService.redeem(getCurrentAccountId(), req.rewardItemId()));
    }

    @GetMapping("/redemptions")
    public ResponseEntity<List<RedemptionResponse>> getRedemptions() {
        return ResponseEntity.ok(rewardService.getRedemptions(getCurrentAccountId()));
    }

    private Long getCurrentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ValidationException("Not authenticated");
        }
        AppUser user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ValidationException("User not found"));
        return user.getAccountId();
    }
}
