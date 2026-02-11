package com.money.draft.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.money.draft.domain.entity.AppUser;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);
}
