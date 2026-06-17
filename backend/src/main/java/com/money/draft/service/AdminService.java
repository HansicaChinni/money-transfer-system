package com.money.draft.service;

import com.money.draft.domain.enums.AccountStatus;
import com.money.draft.dto.AdminAccountDetailResponse;
import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.AdminCreateAccountRequest;
import com.money.draft.dto.RewardLogResponse;
import com.money.draft.dto.TransactionLogResponse;
import java.util.List;

public interface AdminService {
    // Add these new signatures:
    AdminAccountDetailResponse createAccount(AdminCreateAccountRequest req);

    AdminAccountDetailResponse getAccountDetails(Long id);

    AdminAccountDetailResponse updateAccountStatus(Long id, AccountStatus status);

    // Existing signatures:
    List<AdminAccountView> getAllAccounts();
    List<TransactionLogResponse> getAllTransactions();
    List<RewardLogResponse> getAllRewards();
}
