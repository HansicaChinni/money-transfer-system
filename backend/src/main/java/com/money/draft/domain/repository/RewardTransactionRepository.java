
package com.money.draft.domain.repository;

import com.money.draft.domain.entity.RewardTransaction;
import com.money.draft.domain.enums.RewardTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByAccountIdOrderByCreatedOnDesc(Long accountId);

    List<RewardTransaction> findByAccountIdAndTypeOrderByCreatedOnDesc(Long accountId, RewardTransactionType type);

    List<RewardTransaction> findByAccountIdAndReferenceTransactionId(Long accountId, Long referenceTransactionId);

    Optional<RewardTransaction> findByAccountIdAndReferenceTransactionIdAndType(
            Long accountId, Long referenceTransactionId, RewardTransactionType type);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardTransaction r WHERE r.accountId = :accountId AND r.type = :type")
    int sumPointsByAccountIdAndType(@Param("accountId") Long accountId, @Param("type") RewardTransactionType type);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardTransaction r WHERE r.type = :type")
    int sumPointsByType(@Param("type") RewardTransactionType type);

    @Query("SELECT r FROM RewardTransaction r WHERE r.accountId = :accountId AND r.type = 'EARNED' ORDER BY r.createdOn ASC")
    List<RewardTransaction> findEarnedByAccountIdOrderByCreatedOnAsc(@Param("accountId") Long accountId);

    @Query("SELECT r FROM RewardTransaction r WHERE r.accountId = :accountId AND r.type = 'REDEEMED' ORDER BY r.createdOn ASC")
    List<RewardTransaction> findRedeemedByAccountIdOrderByCreatedOnAsc(@Param("accountId") Long accountId);

    @Query("SELECT DISTINCT r.accountId FROM RewardTransaction r WHERE r.type = 'EARNED' AND r.expiresOn <= :now")
    List<Long> findAccountIdsWithExpiredEarnings(@Param("now") Instant now);

    @Query("SELECT COALESCE(SUM(r.points), 0) FROM RewardTransaction r WHERE r.accountId = :accountId AND r.type = 'EARNED' AND r.expiresOn BETWEEN :from AND :to")
    int sumPointsExpiringBetween(@Param("accountId") Long accountId, @Param("from") Instant from, @Param("to") Instant to);
}
