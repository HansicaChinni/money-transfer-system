
package com.money.draft.dto;

import java.math.BigDecimal;

public class AccountResponse {
    private Long id;
    private String holderName;
    private BigDecimal balance;
    private String status;

    public AccountResponse() { }

    public AccountResponse(Long id, String holderName, BigDecimal balance, String status) {
        this.id = id;
        this.holderName = holderName;
        this.balance = balance;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
