package com.sudesh.ledger.shared.idempotency;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    public IdempotencyService(IdempotencyKeyRepository repository) {
        this.repository = repository;
    }

    public enum ReservationOutcome { ACQUIRED, ALREADY_COMPLETED, IN_PROGRESS }

    public record ReservationResult(ReservationOutcome outcome, IdempotencyKeyEntity existing) {}

    /**
     * Attempts to reserve this key for a brand-new request.
     * ACQUIRED        -> caller should proceed and call markCompleted() afterward.
     * ALREADY_COMPLETED -> caller should replay existing.responseBody/Status verbatim.
     * IN_PROGRESS     -> a concurrent request with the same key is still running; caller should return 409.
     */
    @Transactional
    public ReservationResult reserve(String key, String requestPath) {
        try {
            repository.saveAndFlush(new IdempotencyKeyEntity(key, requestPath, "PROCESSING"));
            return new ReservationResult(ReservationOutcome.ACQUIRED, null);
        } catch (DataIntegrityViolationException e) {
            IdempotencyKeyEntity existing = repository.findByIdempotencyKey(key)
                    .orElseThrow(() -> e); // shouldn't happen — the insert just failed because it exists
            ReservationOutcome outcome = "COMPLETED".equals(existing.getStatus())
                    ? ReservationOutcome.ALREADY_COMPLETED
                    : ReservationOutcome.IN_PROGRESS;
            return new ReservationResult(outcome, existing);
        }
    }

    @Transactional
    public void markCompleted(String key, int responseStatus, String responseBody) {
        IdempotencyKeyEntity entity = repository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency key vanished: " + key));
        entity.setStatus("COMPLETED");
        entity.setResponseStatus(responseStatus);
        entity.setResponseBody(responseBody);
        repository.save(entity);
    }

    /** On failure, remove the reservation so the client can legitimately retry with the same key. */
    @Transactional
    public void releaseOnFailure(String key) {
        repository.deleteById(key);
    }
}