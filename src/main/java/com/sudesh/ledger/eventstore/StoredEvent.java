package com.sudesh.ledger.eventstore;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "event_store")
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateId;
    private String aggregateType;
    private long sequenceNumber;
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String payload;

    private Instant createdAt = Instant.now();

    protected StoredEvent() {}

    public StoredEvent(String aggregateId, String aggregateType,
                        long sequenceNumber, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.payload = payload;
    }

    public Long getId() { return id; }
    public String getAggregateId() { return aggregateId; }
    public String getAggregateType() { return aggregateType; }
    public long getSequenceNumber() { return sequenceNumber; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getCreatedAt() { return createdAt; }
}