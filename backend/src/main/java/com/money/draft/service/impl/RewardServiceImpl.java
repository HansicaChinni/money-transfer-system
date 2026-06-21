package com.money.draft.service.impl;

import com.money.draft.domain.audit.AuditLog;
import com.money.draft.domain.audit.AuditLogRepository;
import com.money.draft.domain.entity.*;
import com.money.draft.domain.enums.RedemptionStatus;
import com.money.draft.domain.enums.TransactionStatus;
import com.money.draft.domain.repository.*;
import com.money.draft.dto.*;
import com.money.draft.exception.*;
import com.money.draft.service.RewardService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RewardServiceImpl implements RewardService {

    private static final Logger log = LoggerFactory.getLogger(RewardServiceImpl.class);
    private static final String COUPON_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRewardsRepository rewardsRepo;
    private final RewardTransactionRepository rewardTxRepo;
    private final RewardItemRepository itemRepo;
    private final RedemptionRequestRepository redemptionRepo;
    private final AccountRepository accountRepo;
    private final AppUserRepository userRepo;
    private final TransactionLogRepository txLogRepo;
    private final RewardConfigRepository configRepo;
    private final AuditLogRepository auditLogRepo;

    @Value("${reward.points-per-unit:100}")
    private int defaultPointsPerUnit;

    public RewardServiceImpl(AccountRewardsRepository rewardsRepo,
                             RewardTransactionRepository rewardTxRepo,
                             RewardItemRepository itemRepo,
                             RedemptionRequestRepository redemptionRepo,
                             AccountRepository accountRepo,
                             AppUserRepository userRepo,
                             TransactionLogRepository txLogRepo,
                             RewardConfigRepository configRepo,
                             AuditLogRepository auditLogRepo) {
        this.rewardsRepo = rewardsRepo;
        this.rewardTxRepo = rewardTxRepo;
        this.itemRepo = itemRepo;
        this.redemptionRepo = redemptionRepo;
        this.accountRepo = accountRepo;
        this.userRepo = userRepo;
        this.txLogRepo = txLogRepo;
        this.configRepo = configRepo;
        this.auditLogRepo = auditLogRepo;
    }

    @PostConstruct
    void seedConfig() {
        if (configRepo.count() == 0) {
            configRepo.save(new RewardConfig(defaultPointsPerUnit));
        }
    }

    private int resolvePointsPerUnit() {
        return configRepo.findById(1L)
                .map(RewardConfig::getPointsPerUnit)
                .orElse(defaultPointsPerUnit);
    }

    @Override
    @Transactional
    public void grantRewardIfEligible(Long fromAccountId, Long toAccountId, Long transactionId, BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("100")) <= 0) return;

        if (Objects.equals(fromAccountId, toAccountId)) return;

        try {
            TransactionLog txLog = txLogRepo.findById(transactionId).orElse(null);
            if (txLog == null || txLog.getStatus() != TransactionStatus.SUCCESS) return;

            AppUser fromUser = userRepo.findByAccountId(fromAccountId).orElse(null);
            AppUser toUser = userRepo.findByAccountId(toAccountId).orElse(null);
            if (fromUser == null || toUser == null || Objects.equals(fromUser.getId(), toUser.getId())) return;

            int ppu = resolvePointsPerUnit();
            int points = amount.intValue() / ppu;
            if (points <= 0) return;

            AccountRewards ar = rewardsRepo.findByAccountId(fromAccountId)
                    .orElseGet(() -> rewardsRepo.save(new AccountRewards(fromAccountId)));

            ar.addPoints(points);
            rewardsRepo.save(ar);

            rewardTxRepo.save(new RewardTransaction(
                    fromAccountId, transactionId, points, amount,
                    "Transferred ₹%s → %d points".formatted(amount, points)));

        } catch (Exception e) {
            log.error("Failed to grant reward for account {} tx {}: {}", fromAccountId, transactionId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RewardSummaryResponse getRewardSummary(Long accountId) {
        AccountRewards ar = rewardsRepo.findByAccountId(accountId).orElse(null);
        if (ar == null) return new RewardSummaryResponse(0, null, null);

        List<RewardTransaction> last = rewardTxRepo.findByAccountIdOrderByCreatedOnDesc(accountId);
        if (last.isEmpty()) return new RewardSummaryResponse(ar.getTotalPoints(), null, null);

        RewardTransaction latest = last.get(0);
        return new RewardSummaryResponse(
                ar.getTotalPoints(),
                latest.getPointsEarned(),
                latest.getCreatedOn().toString()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardTransactionResponse> getRewardHistory(Long accountId) {
        return rewardTxRepo.findByAccountIdOrderByCreatedOnDesc(accountId).stream()
                .map(tx -> new RewardTransactionResponse(
                        tx.getId(), tx.getTransactionId(), tx.getPointsEarned(),
                        tx.getAmount(), tx.getReason(), toLocalDateTime(tx.getCreatedOn())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardItemResponse> getAvailableItems() {
        return itemRepo.findByIsActiveTrue().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RedemptionResponse redeem(Long accountId, Long rewardItemId) {
        RewardItem item = itemRepo.findById(rewardItemId)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Reward item not found"));
        if (!item.isActive()) {
            throw new BusinessException("ITEM_INACTIVE", "This item is no longer available");
        }

        AccountRewards ar = rewardsRepo.findByAccountId(accountId)
                .orElseThrow(() -> new InsufficientRewardPointsException(0, item.getPointsRequired()));

        if (ar.getTotalPoints() < item.getPointsRequired()) {
            throw new InsufficientRewardPointsException(ar.getTotalPoints(), item.getPointsRequired());
        }

        ar.deductPoints(item.getPointsRequired());
        rewardsRepo.save(ar);

        RedemptionRequest rr = new RedemptionRequest(accountId, rewardItemId, item.getPointsRequired());
        rr = redemptionRepo.save(rr);

        return toRedemptionResponse(rr, item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedemptionResponse> getRedemptions(Long accountId) {
        return redemptionRepo.findByAccountIdOrderByCreatedOnDesc(accountId).stream()
                .map(rr -> {
                    RewardItem item = itemRepo.findById(rr.getRewardItemId()).orElse(null);
                    return toRedemptionResponse(rr, item);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RedemptionResponse> getAllRedemptions() {
        return redemptionRepo.findByStatusOrderByCreatedOnDesc(RedemptionStatus.PENDING).stream()
                .map(rr -> {
                    RewardItem item = itemRepo.findById(rr.getRewardItemId()).orElse(null);
                    return toRedemptionResponse(rr, item);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RedemptionResponse fulfillRedemption(Long redemptionId, String notes) {
        RedemptionRequest rr = redemptionRepo.findById(redemptionId)
                .orElseThrow(() -> new BusinessException("REDEMPTION_NOT_FOUND", "Redemption not found"));
        if (rr.getStatus() != RedemptionStatus.PENDING) {
            throw new BusinessException("ALREADY_PROCESSED", "Redemption already " + rr.getStatus().name());
        }

        rr.setStatus(RedemptionStatus.FULFILLED);
        rr.setCouponCode(generateCouponCode());
        rr.setNotes(notes);
        rr.setFulfilledOn(LocalDateTime.now());
        rr = redemptionRepo.save(rr);

        RewardItem item = itemRepo.findById(rr.getRewardItemId()).orElse(null);
        return toRedemptionResponse(rr, item);
    }

    @Override
    @Transactional
    public RedemptionResponse cancelRedemption(Long redemptionId) {
        RedemptionRequest rr = redemptionRepo.findById(redemptionId)
                .orElseThrow(() -> new BusinessException("REDEMPTION_NOT_FOUND", "Redemption not found"));
        if (rr.getStatus() != RedemptionStatus.PENDING) {
            throw new BusinessException("ALREADY_PROCESSED", "Redemption already " + rr.getStatus().name());
        }

        int refundPoints = rr.getPointsSpent();
        AccountRewards ar = rewardsRepo.findByAccountId(rr.getAccountId())
                .orElse(null);
        if (ar != null) {
            ar.addPoints(refundPoints);
            rewardsRepo.save(ar);
        }

        rr.setStatus(RedemptionStatus.CANCELLED);
        rr = redemptionRepo.save(rr);

        RewardItem item = itemRepo.findById(rr.getRewardItemId()).orElse(null);
        return toRedemptionResponse(rr, item);
    }

    @Override
    @Transactional
    public RewardItemResponse createItem(CreateRewardItemRequest req) {
        RewardItem item = new RewardItem(req.name(), req.brand(), req.pointsRequired(), req.couponValue());
        item.setDescription(req.description());
        item.setImageUrl(req.imageUrl());
        item = itemRepo.save(item);
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public RewardItemResponse updateItem(Long id, CreateRewardItemRequest req) {
        RewardItem item = itemRepo.findById(id)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Reward item not found"));
        item.setName(req.name());
        item.setDescription(req.description());
        item.setBrand(req.brand());
        item.setPointsRequired(req.pointsRequired());
        item.setCouponValue(req.couponValue());
        item.setImageUrl(req.imageUrl());
        item = itemRepo.save(item);
        return toItemResponse(item);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        RewardItem item = itemRepo.findById(id)
                .orElseThrow(() -> new BusinessException("ITEM_NOT_FOUND", "Reward item not found"));
        item.setActive(false);
        itemRepo.save(item);
    }

    @Override
    @Transactional(readOnly = true)
    public int getPointsPerUnit() {
        return resolvePointsPerUnit();
    }

    @Override
    @Transactional
    public void updatePointsPerUnit(int pointsPerUnit, String performedBy) {
        if (pointsPerUnit <= 0) {
            throw new ValidationException("Points per unit must be greater than zero");
        }

        RewardConfig config = configRepo.findById(1L)
                .orElse(new RewardConfig(pointsPerUnit));

        int oldValue = config.getPointsPerUnit();
        config.setPointsPerUnit(pointsPerUnit);
        config.setUpdatedBy(performedBy);
        config.setUpdatedAt(LocalDateTime.now());
        configRepo.save(config);

        auditLogRepo.save(new AuditLog(
                "RATIO_CHANGE", "RewardConfig", 1L, performedBy,
                String.valueOf(oldValue), String.valueOf(pointsPerUnit)));
    }

    private String generateCouponCode() {
        long ts = System.currentTimeMillis();
        String tsPart = Long.toString(ts, 36).toUpperCase();
        StringBuilder randomPart = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            randomPart.append(COUPON_CHARS.charAt(RANDOM.nextInt(COUPON_CHARS.length())));
        }
        String code = "RWD-" + tsPart + "-" + randomPart;
        return code.length() > 30 ? code.substring(0, 30) : code;
    }

    private RewardItemResponse toItemResponse(RewardItem item) {
        return new RewardItemResponse(
                item.getId(), item.getName(), item.getDescription(),
                item.getBrand(), item.getPointsRequired(), item.getCouponValue(), item.isActive(),
                item.getImageUrl());
    }

    private RedemptionResponse toRedemptionResponse(RedemptionRequest rr, RewardItem item) {
        return new RedemptionResponse(
                rr.getId(),
                rr.getRewardItemId(),
                item != null ? item.getName() : "Unknown",
                item != null ? item.getBrand() : "",
                rr.getPointsSpent(),
                item != null ? item.getCouponValue() : null,
                rr.getStatus().name(),
                rr.getCouponCode(),
                rr.getNotes(),
                rr.getCreatedOn(),
                rr.getFulfilledOn()
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC).toLocalDateTime();
    }
}
