package com.money.draft.controller;

import com.money.draft.dto.*;
import com.money.draft.service.RewardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rewards")
public class AdminRewardController {

    private final RewardService rewardService;

    public AdminRewardController(RewardService rewardService) {
        this.rewardService = rewardService;
    }

    @GetMapping("/redemptions")
    public ResponseEntity<List<RedemptionResponse>> getRedemptions() {
        return ResponseEntity.ok(rewardService.getAllRedemptions());
    }

    @PatchMapping("/redemptions/{id}/fulfill")
    public ResponseEntity<RedemptionResponse> fulfill(@PathVariable Long id, @Valid @RequestBody FulfillRequest req) {
        return ResponseEntity.ok(rewardService.fulfillRedemption(id, req.notes()));
    }

    @PatchMapping("/redemptions/{id}/cancel")
    public ResponseEntity<RedemptionResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(rewardService.cancelRedemption(id));
    }

    @GetMapping("/items")
    public ResponseEntity<List<RewardItemResponse>> getItems() {
        return ResponseEntity.ok(rewardService.getAvailableItems());
    }

    @PostMapping("/items")
    public ResponseEntity<RewardItemResponse> createItem(@Valid @RequestBody CreateRewardItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rewardService.createItem(req));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<RewardItemResponse> updateItem(@PathVariable Long id, @Valid @RequestBody CreateRewardItemRequest req) {
        return ResponseEntity.ok(rewardService.updateItem(id, req));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        rewardService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}
