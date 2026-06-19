
package com.money.draft.service;

import com.money.draft.dto.TransferRequest;
import com.money.draft.dto.TransferResponse;

import java.math.BigDecimal;

public interface TransferService {
    TransferResponse transfer(TransferRequest request);
    TransferResponse transferForUser(Long fromAccountId, Long toAccountId, BigDecimal amount, boolean useRewardPoints);
}
