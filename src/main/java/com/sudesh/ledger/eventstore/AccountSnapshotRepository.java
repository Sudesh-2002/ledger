package com.sudesh.ledger.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderBySequenceNumberAsc(String aggregateId);
    List<StoredEvent> findAllByOrderByAggregateIdAscSequenceNumberAsc();
    List<StoredEvent> findByAggregateIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
            String aggregateId, long sequenceNumber);

    long countByAggregateId(String aggregateId);
}