
package com.money.draft.service;

import com.money.draft.dto.AccountResponse;
import com.money.draft.dto.TransactionLogResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountResponse getAccount(Long id);
    BigDecimal getBalance(Long id);
    List<TransactionLogResponse> getTransactions(Long accountId);
    void changePassword(Long userId, String currentPassword, String newPassword);
}
