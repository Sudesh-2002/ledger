package com.sudesh.ledger.shared.exception;

/**
 * Thrown when a duplicate Idempotency-Key is detected for a request that
 * is still in progress (status = PROCESSING). The HTTP layer maps this to
 * a 409 Conflict response.
 */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String key) {
        super("A request with idempotency key '" + key + "' is already being processed");
    }
}
