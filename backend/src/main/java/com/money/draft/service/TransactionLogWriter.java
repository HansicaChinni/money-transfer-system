
package com.money.draft.service;

import com.money.draft.domain.entity.TransactionLog;
import com.money.draft.domain.repository.TransactionLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TransactionLogWriter {

    private final TransactionLogRepository txRepo;

    public TransactionLogWriter(TransactionLogRepository txRepo) {
        this.txRepo = txRepo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionLog logSuccess(Long fromId, Long toId, BigDecimal amount, String idempotencyKey,
                                      Integer rewardPointsEarned, Integer rewardPointsUsed) {
        return txRepo.save(TransactionLog.success(fromId, toId, amount, idempotencyKey, rewardPointsEarned, rewardPointsUsed));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionLog logFailure(Long fromId, Long toId, BigDecimal amount, String idempotencyKey, String reason) {
        return txRepo.save(TransactionLog.failure(fromId, toId, amount, idempotencyKey, reason));
    }
}
