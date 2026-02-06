
package com.money.draft.dto;

import java.math.BigDecimal;

public class TransferResponse {
    private String status;         // "SUCCESS" or "FAILED"
    private String message;        // optional human-readable message
    private Long transactionId;    // for successful operations
    private BigDecimal amount;

    public TransferResponse() { }

    public TransferResponse(String status, String message, Long transactionId, BigDecimal amount) {
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
        this.amount = amount;
    }

    public static TransferResponse success(Long txId, BigDecimal amount) {
        return new TransferResponse("SUCCESS", null, txId, amount);
    }

    public static TransferResponse failure(String message) {
        return new TransferResponse("FAILED", message, null, null);
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
