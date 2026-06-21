
package com.money.draft.service.impl;

import com.money.draft.domain.entity.*;
import com.money.draft.domain.enums.RedemptionStatus;
import com.money.draft.domain.repository.*;
import com.money.draft.dto.*;
import com.money.draft.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RewardServiceImplTest {

    @Mock
    private AccountRewardsRepository rewardsRepo;
    @Mock
    private RewardTransactionRepository rewardTxRepo;
    @Mock
    private RewardItemRepository itemRepo;
    @Mock
    private RedemptionRequestRepository redemptionRepo;
    @Mock
    private AccountRepository accountRepo;
    @Mock
    private AppUserRepository userRepo;
    @Mock
    private TransactionLogRepository txLogRepo;
    @Mock
    private RewardConfigRepository configRepo;
    @Mock
    private com.money.draft.domain.audit.AuditLogRepository auditLogRepo;

    @InjectMocks
    private RewardServiceImpl rewardService;

    @BeforeEach
    void setUp() {
        lenient().when(configRepo.findById(1L)).thenReturn(Optional.of(new com.money.draft.domain.entity.RewardConfig(100)));
    }

    /* ---------- grantRewardIfEligible ---------- */

    @Test
    void grantRewardIfEligible_ShouldSkip_WhenAmountBelowThreshold() {
        rewardService.grantRewardIfEligible(1L, 2L, 10L, new BigDecimal("50"));
        verifyNoInteractions(rewardsRepo, rewardTxRepo);
    }

    @Test
    void grantRewardIfEligible_ShouldSkip_WhenAmountEqualsThreshold() {
        rewardService.grantRewardIfEligible(1L, 2L, 10L, new BigDecimal("100"));
        verifyNoInteractions(rewardsRepo, rewardTxRepo);
    }

    @Test
    void grantRewardIfEligible_ShouldSkip_WhenSelfTransfer() {
        rewardService.grantRewardIfEligible(1L, 1L, 10L, new BigDecimal("400"));
        verifyNoInteractions(rewardsRepo, rewardTxRepo);
    }

    @Test
    void grantRewardIfEligible_ShouldSkip_WhenTransactionNotSuccess() {
        TransactionLog failedTx = TransactionLog.failure(1L, 2L, new BigDecimal("400"), "key", "fail");
        when(txLogRepo.findById(10L)).thenReturn(Optional.of(failedTx));

        rewardService.grantRewardIfEligible(1L, 2L, 10L, new BigDecimal("400"));

        verifyNoInteractions(rewardsRepo, rewardTxRepo);
    }

    @Test
    void grantRewardIfEligible_ShouldSkip_WhenSameUser() {
        TransactionLog txLog = TransactionLog.success(1L, 2L, new BigDecimal("400"), "key");
        when(txLogRepo.findById(10L)).thenReturn(Optional.of(txLog));

        AppUser user = new AppUser("user", "pass", com.money.draft.domain.enums.Role.USER, 1L);
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepo.findByAccountId(1L)).thenReturn(Optional.of(user));
        when(userRepo.findByAccountId(2L)).thenReturn(Optional.of(user));

        rewardService.grantRewardIfEligible(1L, 2L, 10L, new BigDecimal("400"));

        verifyNoInteractions(rewardsRepo, rewardTxRepo);
    }

    @Test
    void grantRewardIfEligible_ShouldGrant_WhenEligible() {
        TransactionLog txLog = TransactionLog.success(1L, 2L, new BigDecimal("400"), "key");
        when(txLogRepo.findById(10L)).thenReturn(Optional.of(txLog));

        AppUser fromUser = new AppUser("user1", "pass", com.money.draft.domain.enums.Role.USER, 1L);
        ReflectionTestUtils.setField(fromUser, "id", 1L);
        AppUser toUser = new AppUser("user2", "pass", com.money.draft.domain.enums.Role.USER, 2L);
        ReflectionTestUtils.setField(toUser, "id", 2L);
        when(userRepo.findByAccountId(1L)).thenReturn(Optional.of(fromUser));
        when(userRepo.findByAccountId(2L)).thenReturn(Optional.of(toUser));

        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.empty());
        when(rewardsRepo.save(any(AccountRewards.class))).thenAnswer(inv -> inv.getArgument(0));

        rewardService.grantRewardIfEligible(1L, 2L, 10L, new BigDecimal("400"));

        ArgumentCaptor<AccountRewards> arCaptor = ArgumentCaptor.forClass(AccountRewards.class);
        verify(rewardsRepo, times(2)).save(arCaptor.capture());
        assertEquals(4, arCaptor.getAllValues().get(1).getTotalPoints());

        ArgumentCaptor<RewardTransaction> txCaptor = ArgumentCaptor.forClass(RewardTransaction.class);
        verify(rewardTxRepo).save(txCaptor.capture());
        assertEquals(4, txCaptor.getValue().getPointsEarned());
        assertEquals(10L, txCaptor.getValue().getTransactionId());
        assertEquals(new BigDecimal("400"), txCaptor.getValue().getAmount());
    }

    @Test
    void grantRewardIfEligible_ShouldAccumulatePoints_WhenExisting() {
        TransactionLog txLog = TransactionLog.success(1L, 2L, new BigDecimal("600"), "key");
        when(txLogRepo.findById(11L)).thenReturn(Optional.of(txLog));

        AppUser fromUser = new AppUser("user1", "pass", com.money.draft.domain.enums.Role.USER, 1L);
        ReflectionTestUtils.setField(fromUser, "id", 1L);
        AppUser toUser = new AppUser("user2", "pass", com.money.draft.domain.enums.Role.USER, 2L);
        ReflectionTestUtils.setField(toUser, "id", 2L);
        when(userRepo.findByAccountId(1L)).thenReturn(Optional.of(fromUser));
        when(userRepo.findByAccountId(2L)).thenReturn(Optional.of(toUser));

        AccountRewards existing = new AccountRewards(1L);
        existing.addPoints(5);
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.of(existing));

        rewardService.grantRewardIfEligible(1L, 2L, 11L, new BigDecimal("600"));

        verify(rewardsRepo, times(1)).save(any());
        assertEquals(11, existing.getTotalPoints());
    }

    @Test
    void grantRewardIfEligible_ShouldNotThrow_WhenExceptionThrown() {
        when(txLogRepo.findById(10L)).thenThrow(new RuntimeException("DB error"));

        assertDoesNotThrow(() -> rewardService.grantRewardIfEligible(1L, 2L, 10L, new BigDecimal("600")));
    }

    /* ---------- getRewardSummary ---------- */

    @Test
    void getRewardSummary_ShouldReturnZero_WhenNoRewards() {
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.empty());

        var result = rewardService.getRewardSummary(1L);
        assertEquals(0, result.totalPoints());
        assertNull(result.lastEarned());
        assertNull(result.lastEarnedOn());
    }

    @Test
    void getRewardSummary_ShouldReturnPoints_WhenNoTransactions() {
        AccountRewards ar = new AccountRewards(1L);
        ar.addPoints(10);
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.of(ar));
        when(rewardTxRepo.findByAccountIdOrderByCreatedOnDesc(1L)).thenReturn(List.of());

        var result = rewardService.getRewardSummary(1L);
        assertEquals(10, result.totalPoints());
        assertNull(result.lastEarned());
        assertNull(result.lastEarnedOn());
    }

    @Test
    void getRewardSummary_ShouldReturnFull_WhenTransactionsExist() {
        AccountRewards ar = new AccountRewards(1L);
        ar.addPoints(10);
        RewardTransaction tx = new RewardTransaction(1L, 5L, 3, new BigDecimal("600"), "test");
        ReflectionTestUtils.setField(tx, "createdOn", Instant.now());
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.of(ar));
        when(rewardTxRepo.findByAccountIdOrderByCreatedOnDesc(1L)).thenReturn(List.of(tx));

        var result = rewardService.getRewardSummary(1L);
        assertEquals(10, result.totalPoints());
        assertEquals(3, result.lastEarned());
        assertNotNull(result.lastEarnedOn());
    }

    /* ---------- getRewardHistory ---------- */

    @Test
    void getRewardHistory_ShouldReturnTransactions() {
        RewardTransaction tx = new RewardTransaction(1L, 5L, 3, new BigDecimal("600"), "test");
        ReflectionTestUtils.setField(tx, "createdOn", Instant.now());
        ReflectionTestUtils.setField(tx, "id", 1L);
        when(rewardTxRepo.findByAccountIdOrderByCreatedOnDesc(1L)).thenReturn(List.of(tx));

        var result = rewardService.getRewardHistory(1L);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(5L, result.get(0).transactionId());
        assertEquals(3, result.get(0).pointsEarned());
    }

    /* ---------- getAvailableItems ---------- */

    @Test
    void getAvailableItems_ShouldReturnActiveItems() {
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 1L);
        when(itemRepo.findByIsActiveTrue()).thenReturn(List.of(item));

        var result = rewardService.getAvailableItems();
        assertEquals(1, result.size());
        assertEquals("Gift", result.get(0).name());
    }

    @Test
    void getAvailableItems_ShouldReturnEmpty_WhenNone() {
        when(itemRepo.findByIsActiveTrue()).thenReturn(List.of());
        assertTrue(rewardService.getAvailableItems().isEmpty());
    }

    /* ---------- redeem ---------- */

    @Test
    void redeem_ShouldSucceed_WhenSufficientPoints() {
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 1L);
        AccountRewards ar = new AccountRewards(1L);
        ar.addPoints(200);
        RedemptionRequest saved = new RedemptionRequest(1L, 1L, 100);
        ReflectionTestUtils.setField(saved, "id", 10L);
        ReflectionTestUtils.setField(saved, "createdOn", Instant.now());

        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.of(ar));
        when(redemptionRepo.save(any())).thenReturn(saved);

        var result = rewardService.redeem(1L, 1L);
        assertEquals(10L, result.id());
        assertEquals(100, ar.getTotalPoints());
    }

    @Test
    void redeem_ShouldThrow_WhenItemNotFound() {
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> rewardService.redeem(1L, 99L));
    }

    @Test
    void redeem_ShouldThrow_WhenItemInactive() {
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        item.setActive(false);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        assertThrows(BusinessException.class, () -> rewardService.redeem(1L, 1L));
    }

    @Test
    void redeem_ShouldThrow_WhenNoRewardsRecord() {
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 1L);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.empty());
        assertThrows(InsufficientRewardPointsException.class, () -> rewardService.redeem(1L, 1L));
    }

    @Test
    void redeem_ShouldThrow_WhenInsufficientPoints() {
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 1L);
        AccountRewards ar = new AccountRewards(1L);
        ar.addPoints(50);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.of(ar));
        assertThrows(InsufficientRewardPointsException.class, () -> rewardService.redeem(1L, 1L));
    }

    /* ---------- getRedemptions ---------- */

    @Test
    void getRedemptions_ShouldReturnList() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        ReflectionTestUtils.setField(rr, "id", 10L);
        ReflectionTestUtils.setField(rr, "createdOn", Instant.now());
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 5L);

        when(redemptionRepo.findByAccountIdOrderByCreatedOnDesc(1L)).thenReturn(List.of(rr));
        when(itemRepo.findById(5L)).thenReturn(Optional.of(item));

        var result = rewardService.getRedemptions(1L);
        assertEquals(1, result.size());
        assertEquals("Gift", result.get(0).itemName());
    }

    @Test
    void getRedemptions_ShouldHandleMissingItem() {
        RedemptionRequest rr = new RedemptionRequest(1L, 99L, 100);
        ReflectionTestUtils.setField(rr, "id", 10L);
        ReflectionTestUtils.setField(rr, "createdOn", Instant.now());

        when(redemptionRepo.findByAccountIdOrderByCreatedOnDesc(1L)).thenReturn(List.of(rr));
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());

        var result = rewardService.getRedemptions(1L);
        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).itemName());
    }

    /* ---------- getAllRedemptions ---------- */

    @Test
    void getAllRedemptions_ShouldReturnPendingOnly() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        ReflectionTestUtils.setField(rr, "id", 10L);
        ReflectionTestUtils.setField(rr, "createdOn", Instant.now());

        when(redemptionRepo.findByStatusOrderByCreatedOnDesc(RedemptionStatus.PENDING)).thenReturn(List.of(rr));
        when(itemRepo.findById(5L)).thenReturn(Optional.empty());

        var result = rewardService.getAllRedemptions();
        assertEquals(1, result.size());
    }

    /* ---------- fulfillRedemption ---------- */

    @Test
    void fulfillRedemption_ShouldSucceed() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        ReflectionTestUtils.setField(rr, "id", 10L);
        ReflectionTestUtils.setField(rr, "createdOn", Instant.now());
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 5L);

        when(redemptionRepo.findById(10L)).thenReturn(Optional.of(rr));
        when(redemptionRepo.save(any())).thenReturn(rr);
        when(itemRepo.findById(5L)).thenReturn(Optional.of(item));

        var result = rewardService.fulfillRedemption(10L, "thanks");
        assertEquals(RedemptionStatus.FULFILLED.name(), result.status());
        assertNotNull(result.couponCode());
        assertEquals("thanks", result.notes());
    }

    @Test
    void fulfillRedemption_ShouldThrow_WhenNotFound() {
        when(redemptionRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> rewardService.fulfillRedemption(99L, "notes"));
    }

    @Test
    void fulfillRedemption_ShouldThrow_WhenAlreadyProcessed() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        rr.setStatus(RedemptionStatus.FULFILLED);
        when(redemptionRepo.findById(10L)).thenReturn(Optional.of(rr));
        assertThrows(BusinessException.class, () -> rewardService.fulfillRedemption(10L, "notes"));
    }

    /* ---------- cancelRedemption ---------- */

    @Test
    void cancelRedemption_ShouldRefundAndCancel() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        ReflectionTestUtils.setField(rr, "id", 10L);
        ReflectionTestUtils.setField(rr, "createdOn", Instant.now());
        AccountRewards ar = new AccountRewards(1L);
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 5L);

        when(redemptionRepo.findById(10L)).thenReturn(Optional.of(rr));
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.of(ar));
        when(redemptionRepo.save(any())).thenReturn(rr);
        when(itemRepo.findById(5L)).thenReturn(Optional.of(item));

        var result = rewardService.cancelRedemption(10L);
        assertEquals(RedemptionStatus.CANCELLED.name(), result.status());
        assertEquals(100, ar.getTotalPoints());
    }

    @Test
    void cancelRedemption_ShouldHandleMissingRewardsRecord() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        ReflectionTestUtils.setField(rr, "id", 10L);
        ReflectionTestUtils.setField(rr, "createdOn", Instant.now());
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 5L);

        when(redemptionRepo.findById(10L)).thenReturn(Optional.of(rr));
        when(rewardsRepo.findByAccountId(1L)).thenReturn(Optional.empty());
        when(redemptionRepo.save(any())).thenReturn(rr);
        when(itemRepo.findById(5L)).thenReturn(Optional.of(item));

        var result = rewardService.cancelRedemption(10L);
        assertEquals(RedemptionStatus.CANCELLED.name(), result.status());
    }

    @Test
    void cancelRedemption_ShouldThrow_WhenNotFound() {
        when(redemptionRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> rewardService.cancelRedemption(99L));
    }

    @Test
    void cancelRedemption_ShouldThrow_WhenAlreadyProcessed() {
        RedemptionRequest rr = new RedemptionRequest(1L, 5L, 100);
        rr.setStatus(RedemptionStatus.FULFILLED);
        when(redemptionRepo.findById(10L)).thenReturn(Optional.of(rr));
        assertThrows(BusinessException.class, () -> rewardService.cancelRedemption(10L));
    }

    /* ---------- createItem ---------- */

    @Test
    void createItem_ShouldCreateAndReturn() {
        var req = new CreateRewardItemRequest("Gift", "desc", "Brand", 100, new BigDecimal("50"), "img.jpg");
        RewardItem saved = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(saved, "id", 1L);
        saved.setDescription("desc");
        saved.setImageUrl("img.jpg");

        when(itemRepo.save(any())).thenReturn(saved);

        var result = rewardService.createItem(req);
        assertEquals("Gift", result.name());
        assertEquals("Brand", result.brand());
        assertEquals("img.jpg", result.imageUrl());
    }

    /* ---------- updateItem ---------- */

    @Test
    void updateItem_ShouldUpdateAndReturn() {
        RewardItem existing = new RewardItem("Old", "OldBrand", 50, new BigDecimal("25"));
        ReflectionTestUtils.setField(existing, "id", 1L);
        var req = new CreateRewardItemRequest("New", "new desc", "NewBrand", 100, new BigDecimal("50"), "new.jpg");

        when(itemRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(itemRepo.save(any())).thenReturn(existing);

        var result = rewardService.updateItem(1L, req);
        assertEquals("New", result.name());
        assertEquals("NewBrand", result.brand());
        assertEquals("new.jpg", result.imageUrl());
    }

    @Test
    void updateItem_ShouldThrow_WhenNotFound() {
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());
        var req = new CreateRewardItemRequest("N", null, "B", 100, new BigDecimal("50"), null);
        assertThrows(BusinessException.class, () -> rewardService.updateItem(99L, req));
    }

    /* ---------- deleteItem ---------- */

    @Test
    void deleteItem_ShouldDeactivate() {
        RewardItem item = new RewardItem("Gift", "Brand", 100, new BigDecimal("50"));
        ReflectionTestUtils.setField(item, "id", 1L);
        when(itemRepo.findById(1L)).thenReturn(Optional.of(item));

        rewardService.deleteItem(1L);
        assertFalse(item.isActive());
        verify(itemRepo).save(item);
    }

    @Test
    void deleteItem_ShouldThrow_WhenNotFound() {
        when(itemRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> rewardService.deleteItem(99L));
    }
}
