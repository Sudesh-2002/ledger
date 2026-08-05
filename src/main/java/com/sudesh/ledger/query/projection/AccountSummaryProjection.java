package com.sudesh.ledger.query.projection;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "account_summary")
public class AccountSummaryProjection {

    @Id
    @Column(name = "account_id")
    private String accountId;

    private String ownerName;
    private BigDecimal balance;
    private String status;
    private long version;

    protected AccountSummaryProjection() {}

    public AccountSummaryProjection(String accountId, String ownerName,
                                     BigDecimal balance, String status, long version) {
        this.accountId = accountId;
        this.ownerName = ownerName;
        this.balance = balance;
        this.status = status;
        this.version = version;
    }

    public String getAccountId() { return accountId; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getStatus() { return status; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}