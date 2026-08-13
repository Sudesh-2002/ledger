package com.sudesh.ledger.command.domain;

import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.domain.event.AccountOpened;
import com.sudesh.ledger.command.domain.event.MoneyDeposited;
import com.sudesh.ledger.command.domain.event.MoneyWithdrawn;
import com.sudesh.ledger.command.domain.exception.InsufficientFundsException;
import com.sudesh.ledger.eventstore.AccountSnapshot;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Account {

    private String accountId;
    private String ownerName;
    private BigDecimal balance = BigDecimal.ZERO;
    private AccountStatus status;
    private long version = 0; // equals number of events applied so far

    // uncommitted events produced by the current command, awaiting persistence
    private final List<Object> pendingEvents = new ArrayList<>();

    private Account() {}

    // ---- Factory: handle a command with no prior history ----
    public static Account open(OpenAccountCommand command) {
        Account account = new Account();
        AccountOpened event = new AccountOpened(
                command.accountId(), command.ownerName(), command.openingBalance());
        account.raise(event);
        return account;
    }

    // ---- Rehydrate: rebuild an existing aggregate purely from history ----
    public static Account replay(List<Object> history) {
        Account account = new Account();
        for (Object event : history) {
            account.apply(event);
            account.version++;
        }
        return account;
    }

    // ---- Command handlers: validate against current state, raise events ----
    public void deposit(DepositCommand command) {
        requireOpen();
        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        raise(new MoneyDeposited(accountId, command.amount(), command.reference()));
    }

    public void withdraw(WithdrawCommand command) {
        requireOpen();
        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance.compareTo(command.amount()) < 0) {
            throw new InsufficientFundsException(accountId);
        }
        raise(new MoneyWithdrawn(accountId, command.amount(), command.reference()));
    }

    private void requireOpen() {
        if (status != AccountStatus.OPEN) {
            throw new IllegalStateException("Account " + accountId + " is not open");
        }
    }

    // ---- raise: record a new event AND immediately apply it, so subsequent
    // commands in the same call see up-to-date state ----
    private void raise(Object event) {
        apply(event);
        pendingEvents.add(event);
    }

    // ---- apply: the ONLY place state is mutated. One branch per event type. ----
    private void apply(Object event) {
        switch (event) {
            case AccountOpened e -> {
                this.accountId = e.accountId();
                this.ownerName = e.ownerName();
                this.balance = e.openingBalance();
                this.status = AccountStatus.OPEN;
            }
            case MoneyDeposited e -> this.balance = this.balance.add(e.amount());
            case MoneyWithdrawn e -> this.balance = this.balance.subtract(e.amount());
            default -> throw new IllegalArgumentException(
                    "Unknown event type: " + event.getClass());
        }
    }

    public static Account restoreFromSnapshot(AccountSnapshot snapshot, List<Object> eventsSinceSnapshot) {
        Account account = new Account();
        account.accountId = snapshot.getAggregateId();
        account.ownerName = snapshot.getOwnerName();
        account.balance = snapshot.getBalance();
        account.status = AccountStatus.valueOf(snapshot.getStatus());
        account.version = snapshot.getVersion();

        for (Object event : eventsSinceSnapshot) {
            account.apply(event);
            account.version++;
        }
        return account;
    }

    public AccountSnapshot toSnapshot() {
        return new AccountSnapshot(accountId, version, ownerName, balance, status.name());
    }

    public List<Object> getPendingEvents() { return List.copyOf(pendingEvents); }
    public void clearPendingEvents() { pendingEvents.clear(); }

    public String getAccountId() { return accountId; }
    public BigDecimal getBalance() { return balance; }
    public long getVersion() { return version; }
    public AccountStatus getStatus() { return status; }
}