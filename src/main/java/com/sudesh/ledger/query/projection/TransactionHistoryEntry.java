package com.sudesh.ledger.query.projection;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_history")
public class TransactionHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountId;
    private String type;
    private BigDecimal amount;
    private String reference;
    private BigDecimal balanceAfter;
    private Instant occurredAt = Instant.now();

    protected TransactionHistoryEntry() {}

    public TransactionHistoryEntry(String accountId, String type, BigDecimal amount,
                                    String reference, BigDecimal balanceAfter) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.balanceAfter = balanceAfter;
    }

    public String getAccountId() { return accountId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public String getReference() { return reference; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Instant getOccurredAt() { return occurredAt; }
}