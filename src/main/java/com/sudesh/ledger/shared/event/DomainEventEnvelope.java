package com.sudesh.ledger.shared.event;

public record DomainEventEnvelope(String aggregateId, long sequenceNumber, Object payload) {}