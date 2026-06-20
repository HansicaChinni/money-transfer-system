
package com.money.draft.dto;

import java.time.LocalDateTime;

public record RewardTransactionResponse(
        Long id,
        Long accountId,
        String accountNumber,
        String holderName,
        String type,
        int points,
        Long referenceTransactionId,
        LocalDateTime createdOn,
        LocalDateTime expiresOn
) {}
