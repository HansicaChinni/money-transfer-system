
package com.money.draft.domain.repository;

import com.money.draft.domain.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, Long> {
    Optional<TransactionLog> findByIdempotencyKey(String idempotencyKey);

    List<TransactionLog> findByFromAccountIdOrToAccountIdOrderByCreatedOnDesc(Long fromId, Long toId);

}
