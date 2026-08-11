package com.sudesh.ledger.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudesh.ledger.command.domain.Account;
import com.sudesh.ledger.command.domain.AccountEventCodec;
import com.sudesh.ledger.command.domain.command.DepositCommand;
import com.sudesh.ledger.command.domain.command.OpenAccountCommand;
import com.sudesh.ledger.command.domain.command.WithdrawCommand;
import com.sudesh.ledger.command.domain.exception.AccountNotFoundException;
import com.sudesh.ledger.eventstore.EventStore;
import com.sudesh.ledger.eventstore.StoredEvent;
import com.sudesh.ledger.shared.event.DomainEventEnvelope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.sudesh.ledger.config.KafkaTopicConfig.ACCOUNT_EVENTS_TOPIC;

@Service
public class AccountCommandService {

    private final EventStore eventStore;
    private final AccountEventCodec codec;
    private final KafkaTemplate<String, DomainEventEnvelope> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String AGGREGATE_TYPE = "Account";

    public AccountCommandService(EventStore eventStore, AccountEventCodec codec,
                                  KafkaTemplate<String, DomainEventEnvelope> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.eventStore = eventStore;
        this.codec = codec;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
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
        // 1. Durable write to source of truth FIRST
        eventStore.append(accountId, AGGREGATE_TYPE, baseVersion, events);

        // 2. Only then publish — if this fails or the app crashes here,
        //    the event store still has the truth; a republish job (Step 9-ish)
        //    or manual rebuild can recover the read side.
        long sequence = baseVersion + 1;
        for (Object event : events) {
            try {
                String payloadJson = objectMapper.writeValueAsString(event);
                DomainEventEnvelope envelope = new DomainEventEnvelope(
                        accountId, sequence, event.getClass().getSimpleName(), payloadJson);
                kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, accountId, envelope);
            } catch (Exception e) {
                throw new RuntimeException("Failed to publish event to Kafka", e);
            }
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