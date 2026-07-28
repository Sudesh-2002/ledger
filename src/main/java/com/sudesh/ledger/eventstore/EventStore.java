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
    private final ObjectMapper objectMapper;

    public EventStore(EventStoreRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void append(String aggregateId, String aggregateType,
                        long expectedVersion, List<Object> newEvents) {
        long nextSequence = expectedVersion + 1;
        try {
            for (Object event : newEvents) {
                String payload = objectMapper.writeValueAsString(event);
                StoredEvent stored = new StoredEvent(
                        aggregateId, aggregateType, nextSequence,
                        event.getClass().getSimpleName(), payload);
                repository.saveAndFlush(stored);
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

    public long currentVersion(String aggregateId) {
        return repository.countByAggregateId(aggregateId);
    }
}