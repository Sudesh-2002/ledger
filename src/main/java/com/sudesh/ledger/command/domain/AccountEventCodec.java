package com.sudesh.ledger.command.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudesh.ledger.command.domain.event.AccountOpened;
import com.sudesh.ledger.command.domain.event.MoneyDeposited;
import com.sudesh.ledger.command.domain.event.MoneyWithdrawn;
import com.sudesh.ledger.eventstore.StoredEvent;
import org.springframework.stereotype.Component;

@Component
public class AccountEventCodec {

    private final ObjectMapper objectMapper;

    public AccountEventCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object toDomainEvent(StoredEvent stored) {
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