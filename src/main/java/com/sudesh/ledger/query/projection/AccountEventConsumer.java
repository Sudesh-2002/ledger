package com.sudesh.ledger.query.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudesh.ledger.command.domain.event.AccountOpened;
import com.sudesh.ledger.command.domain.event.MoneyDeposited;
import com.sudesh.ledger.command.domain.event.MoneyWithdrawn;
import com.sudesh.ledger.shared.event.DomainEventEnvelope;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountEventConsumer {

    private final AccountProjector projector;
    private final ObjectMapper objectMapper;

    public AccountEventConsumer(AccountProjector projector, ObjectMapper objectMapper) {
        this.projector = projector;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "account-events", groupId = "ledger-projector")
    @Transactional
    public void consume(ConsumerRecord<String, DomainEventEnvelope> record, Acknowledgment ack) {
        DomainEventEnvelope envelope = record.value();
        Object domainEvent = deserializePayload(envelope);

        // Reuse the exact same apply() logic Step 4's rebuild service uses —
        // one projection code path regardless of whether events arrive live or via replay.
        projector.apply(new AccountProjectorEnvelope(envelope.aggregateId(), envelope.sequenceNumber(), domainEvent));

        ack.acknowledge(); // commit Kafka offset only after the DB write above succeeded
    }

    private Object deserializePayload(DomainEventEnvelope envelope) {
        try {
            Class<?> type = switch (envelope.eventType()) {
                case "AccountOpened" -> AccountOpened.class;
                case "MoneyDeposited" -> MoneyDeposited.class;
                case "MoneyWithdrawn" -> MoneyWithdrawn.class;
                default -> throw new IllegalArgumentException("Unknown event type: " + envelope.eventType());
            };
            return objectMapper.readValue(envelope.payloadJson(), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event from Kafka", e);
        }
    }
}