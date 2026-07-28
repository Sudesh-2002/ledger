package com.sudesh.ledger.shared.exception;

public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String aggregateId, long expectedVersion) {
        super("Concurrency conflict for aggregate " + aggregateId
                + " at expected version " + expectedVersion);
    }
}