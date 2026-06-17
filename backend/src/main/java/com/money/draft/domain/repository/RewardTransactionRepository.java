package com.money.draft.domain.repository;

import com.money.draft.domain.entity.RewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByAccountIdOrderByCreatedOnDesc(Long accountId);
}
