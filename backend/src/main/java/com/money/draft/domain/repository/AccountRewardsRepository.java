package com.money.draft.domain.repository;

import com.money.draft.domain.entity.AccountRewards;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRewardsRepository extends JpaRepository<AccountRewards, Long> {
    Optional<AccountRewards> findByAccountId(Long accountId);
}
