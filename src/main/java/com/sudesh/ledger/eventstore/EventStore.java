package com.sudesh.ledger.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudesh.ledger.shared.exception.ConcurrencyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventStore {

    private final EventStoreRepository repository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public EventStore(EventStoreRepository repository, OutboxRepository outboxRepository,
                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void append(String aggregateId, String aggregateType,
                        long expectedVersion, List<Object> newEvents) {
        long nextSequence = expectedVersion + 1;
        try {
            for (Object event : newEvents) {
                String payload = objectMapper.writeValueAsString(event);
                String eventType = event.getClass().getSimpleName();

                StoredEvent stored = new StoredEvent(
                        aggregateId, aggregateType, nextSequence, eventType, payload);
                repository.saveAndFlush(stored);

                // same transaction, same DB — this is what makes it atomic with the event write
                outboxRepository.save(new OutboxEntry(aggregateId, nextSequence, eventType, payload));

                nextSequence++;
            }
        } catch (DataIntegrityViolationException e) {
            throw new ConcurrencyException(aggregateId, expectedVersion);
        } catch (Exception e) {
            throw new RuntimeException("Failed to append events", e);
        }
    }

    public List<StoredEvent> loadEvents(String aggregateId) {
        return repository.findByAggregateIdOrderBySequenceNumberAsc(aggregateId);
    }

    public List<StoredEvent> loadEventsAfter(String aggregateId, long afterVersion) {
        return repository.findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                aggregateId, afterVersion);
    }

    public long currentVersion(String aggregateId) {
        return repository.countByAggregateId(aggregateId);
    }
}