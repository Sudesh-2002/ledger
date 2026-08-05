package com.sudesh.ledger.command.service;

import com.sudesh.ledger.command.domain.Account;
import com.sudesh.ledger.command.domain.AccountEventCodec;
import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.domain.exception.AccountNotFoundException;
import com.sudesh.ledger.eventstore.EventStore;
import com.sudesh.ledger.eventstore.StoredEvent;
import com.sudesh.ledger.shared.event.DomainEventEnvelope;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountCommandService {

    private final EventStore eventStore;
    private final AccountEventCodec codec;
    private final ApplicationEventPublisher publisher;
    private static final String AGGREGATE_TYPE = "Account";

    public AccountCommandService(EventStore eventStore, AccountEventCodec codec,
                                  ApplicationEventPublisher publisher) {
        this.eventStore = eventStore;
        this.codec = codec;
        this.publisher = publisher;
    }

    public void openAccount(OpenAccountCommand command) {
        Account account = Account.open(command);
        appendAndPublish(command.accountId(), 0, account.getPendingEvents());
    }

    public void deposit(DepositCommand command) {
        Account account = loadAccount(command.accountId());
        account.deposit(command);
        appendAndPublish(command.accountId(), account.getVersion(), account.getPendingEvents());
    }

    public void withdraw(WithdrawCommand command) {
        Account account = loadAccount(command.accountId());
        account.withdraw(command);
        appendAndPublish(command.accountId(), account.getVersion(), account.getPendingEvents());
    }

    private void appendAndPublish(String accountId, long baseVersion, List<Object> events) {
        eventStore.append(accountId, AGGREGATE_TYPE, baseVersion, events);
        long sequence = baseVersion + 1;
        for (Object event : events) {
            publisher.publishEvent(new DomainEventEnvelope(accountId, sequence, event));
            sequence++;
        }
    }

    private Account loadAccount(String accountId) {
        List<StoredEvent> stored = eventStore.loadEvents(accountId);
        if (stored.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        List<Object> history = stored.stream().map(codec::toDomainEvent).toList();
        return Account.replay(history);
    }
}