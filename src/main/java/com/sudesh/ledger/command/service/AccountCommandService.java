package com.sudesh.ledger.command.service;

import com.sudesh.ledger.command.domain.Account;
import com.sudesh.ledger.command.domain.AccountEventCodec;
import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.domain.exception.AccountNotFoundException;
import com.sudesh.ledger.eventstore.AccountSnapshot;
import com.sudesh.ledger.eventstore.EventStore;
import com.sudesh.ledger.eventstore.SnapshotStore;
import com.sudesh.ledger.eventstore.StoredEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AccountCommandService {

    private final EventStore eventStore;
    private final SnapshotStore snapshotStore;
    private final AccountEventCodec codec;
    private static final String AGGREGATE_TYPE = "Account";

    public AccountCommandService(EventStore eventStore, SnapshotStore snapshotStore,
                                  AccountEventCodec codec) {
        this.eventStore = eventStore;
        this.snapshotStore = snapshotStore;
        this.codec = codec;
    }

    public void openAccount(OpenAccountCommand command) {
        Account account = Account.open(command);
        appendAndPublish(command.accountId(), 0, account.getPendingEvents());
        maybeSnapshot(account);
    }

    public void deposit(DepositCommand command) {
        Account account = loadAccount(command.accountId());
        account.deposit(command);
        appendAndPublish(command.accountId(), account.getVersion(), account.getPendingEvents());
        maybeSnapshot(account);
    }

    public void withdraw(WithdrawCommand command) {
        Account account = loadAccount(command.accountId());
        account.withdraw(command);
        appendAndPublish(command.accountId(), account.getVersion(), account.getPendingEvents());
        maybeSnapshot(account);
    }

    // "publish" now just means: append to the event store, which atomically
    // writes the outbox row in the same transaction (see EventStore.append()).
    // OutboxPublisher (a separate scheduled poller) delivers to Kafka from there —
    // this service no longer talks to Kafka directly at all.
    private void appendAndPublish(String accountId, long baseVersion, List<Object> events) {
        eventStore.append(accountId, AGGREGATE_TYPE, baseVersion, events);
    }

    private Account loadAccount(String accountId) {
        Optional<AccountSnapshot> snapshot = snapshotStore.loadLatest(accountId);

        if (snapshot.isPresent()) {
            List<StoredEvent> stored = eventStore.loadEventsAfter(accountId, snapshot.get().getVersion());
            List<Object> events = stored.stream().map(codec::toDomainEvent).toList();
            return Account.restoreFromSnapshot(snapshot.get(), events);
        }

        List<StoredEvent> stored = eventStore.loadEvents(accountId);
        if (stored.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        List<Object> events = stored.stream().map(codec::toDomainEvent).toList();
        return Account.replay(events);
    }

    private void maybeSnapshot(Account account) {
        snapshotStore.saveIfDue(account.getAccountId(), account.getVersion(), account.toSnapshot());
    }
}