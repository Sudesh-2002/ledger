package com.sudesh.ledger.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudesh.ledger.command.domain.Account;
import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.domain.event.AccountOpened;
import com.sudesh.ledger.command.domain.event.MoneyDeposited;
import com.sudesh.ledger.command.domain.event.MoneyWithdrawn;
import com.sudesh.ledger.command.domain.exception.AccountNotFoundException;
import com.sudesh.ledger.eventstore.EventStore;
import com.sudesh.ledger.eventstore.StoredEvent;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountCommandService {

    private final EventStore eventStore;
    private final ObjectMapper objectMapper;
    private static final String AGGREGATE_TYPE = "Account";

    public AccountCommandService(EventStore eventStore, ObjectMapper objectMapper) {
        this.eventStore = eventStore;
        this.objectMapper = objectMapper;
    }

    public void openAccount(OpenAccountCommand command) {
        Account account = Account.open(command);
        eventStore.append(command.accountId(), AGGREGATE_TYPE, 0, account.getPendingEvents());
    }

    public void deposit(DepositCommand command) {
        Account account = loadAccount(command.accountId());
        account.deposit(command);
        eventStore.append(command.accountId(), AGGREGATE_TYPE, account.getVersion(), account.getPendingEvents());
    }

    public void withdraw(WithdrawCommand command) {
        Account account = loadAccount(command.accountId());
        account.withdraw(command);
        eventStore.append(command.accountId(), AGGREGATE_TYPE, account.getVersion(), account.getPendingEvents());
    }

    private Account loadAccount(String accountId) {
        List<StoredEvent> stored = eventStore.loadEvents(accountId);
        if (stored.isEmpty()) {
            throw new AccountNotFoundException(accountId);
        }
        List<Object> history = stored.stream().map(this::deserialize).toList();
        return Account.replay(history);
    }

    private Object deserialize(StoredEvent stored) {
        try {
            Class<?> type = switch (stored.getEventType()) {
                case "AccountOpened" -> AccountOpened.class;
                case "MoneyDeposited" -> MoneyDeposited.class;
                case "MoneyWithdrawn" -> MoneyWithdrawn.class;
                default -> throw new IllegalArgumentException("Unknown event type: " + stored.getEventType());
            };
            return objectMapper.readValue(stored.getPayload(), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}