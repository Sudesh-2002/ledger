package com.sudesh.ledger.query.projection;

public record AccountProjectorEnvelope(String aggregateId, long sequenceNumber, Object payload) {}