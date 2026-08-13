package com.sudesh.ledger.eventstore;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "account_snapshot")
public class AccountSnapshot {

    @Id
    @Column(name = "aggregate_id")
    private String aggregateId;

    private long version;
    private String ownerName;
    private BigDecimal balance;
    private String status;
    private Instant createdAt = Instant.now();

    protected AccountSnapshot() {}

    public AccountSnapshot(String aggregateId, long version, String ownerName,
                            BigDecimal balance, String status) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.ownerName = ownerName;
        this.balance = balance;
        this.status = status;
    }

    public String getAggregateId() { return aggregateId; }
    public long getVersion() { return version; }
    public String getOwnerName() { return ownerName; }
    public BigDecimal getBalance() { return balance; }
    public String getStatus() { return status; }
}