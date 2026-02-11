
package com.money.draft.service;

import com.money.draft.dto.AdminAccountView;
import com.money.draft.dto.TransactionLogResponse;

import java.util.List;

public interface AdminService {
    List<AdminAccountView> getAllAccounts();
    List<TransactionLogResponse> getAllTransactions();
}

