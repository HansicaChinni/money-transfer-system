
package com.money.draft.controller;

import com.money.draft.dto.RewardSummaryResponse;
import com.money.draft.dto.RewardTransactionResponse;
import com.money.draft.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin Rewards", description = "Admin portal: reward oversight")
@RestController
@RequestMapping("/admin/rewards")
@SecurityRequirement(name = "BearerAuth")
public class AdminRewardController {

    private final RewardService rewardService;

    public AdminRewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @Operation(summary = "List all reward transactions across all accounts")
    @GetMapping
    public ResponseEntity<List<RewardTransactionResponse>> allRewardTransactions() {
        return ResponseEntity.ok(rewardService.getAllRewardTransactions());
    }

    @Operation(summary = "Get reward summary for a specific account")
    @GetMapping("/{accountId}")
    public ResponseEntity<RewardSummaryResponse> rewardSummaryForAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(rewardService.getRewardSummaryForAdmin(accountId));
    }
}
