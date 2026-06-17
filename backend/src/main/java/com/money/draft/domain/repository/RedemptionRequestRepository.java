package com.money.draft.domain.repository;

import com.money.draft.domain.entity.RedemptionRequest;
import com.money.draft.domain.enums.RedemptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedemptionRequestRepository extends JpaRepository<RedemptionRequest, Long> {
    List<RedemptionRequest> findByAccountIdOrderByCreatedOnDesc(Long accountId);
    List<RedemptionRequest> findByStatusOrderByCreatedOnDesc(RedemptionStatus status);
}
