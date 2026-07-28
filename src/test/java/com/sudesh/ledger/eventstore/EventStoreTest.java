package com.sudesh.ledger.eventstore;

import com.sudesh.ledger.shared.exception.ConcurrencyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

record DummyEvent(String message) {}

@SpringBootTest
class EventStoreTest {

    @Autowired
    private EventStore eventStore;

    @Test
    void appendsAndReplaysEventsInOrder() {
        String aggregateId = UUID.randomUUID().toString();

        eventStore.append(aggregateId, "Dummy", 0,
                List.of(new DummyEvent("first"), new DummyEvent("second")));

        List<StoredEvent> events = eventStore.loadEvents(aggregateId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getSequenceNumber()).isEqualTo(1);
        assertThat(events.get(1).getSequenceNumber()).isEqualTo(2);
    }

    @Test
    void rejectsStaleExpectedVersion() {
        String aggregateId = UUID.randomUUID().toString();
        eventStore.append(aggregateId, "Dummy", 0, List.of(new DummyEvent("first")));

        assertThatThrownBy(() ->
                eventStore.append(aggregateId, "Dummy", 0, List.of(new DummyEvent("conflict")))
        ).isInstanceOf(ConcurrencyException.class);
    }
}