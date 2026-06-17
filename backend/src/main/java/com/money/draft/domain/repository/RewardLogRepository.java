package com.money.draft.domain.repository;

import com.money.draft.domain.entity.RewardLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RewardLogRepository extends JpaRepository<RewardLog, Long> {
    Optional<RewardLog> findByTransactionId(Long transactionId);

    List<RewardLog> findByAccountIdOrderByCreatedOnDesc(Long accountId);
}
