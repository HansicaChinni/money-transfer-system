package com.money.draft.domain.repository;

import com.money.draft.domain.entity.RewardItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {
    List<RewardItem> findByIsActiveTrue();
}
