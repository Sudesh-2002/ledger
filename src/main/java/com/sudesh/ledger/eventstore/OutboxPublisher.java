package com.sudesh.ledger.eventstore;

import com.sudesh.ledger.shared.event.DomainEventEnvelope;
import com.sudesh.ledger.shared.metrics.LedgerMetrics;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.sudesh.ledger.config.KafkaTopicConfig.ACCOUNT_EVENTS_TOPIC;

@Component
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEventEnvelope> kafkaTemplate;
    private final LedgerMetrics metrics;
    private final CircuitBreaker circuitBreaker;

    public OutboxPublisher(OutboxRepository outboxRepository,
                            KafkaTemplate<String, DomainEventEnvelope> kafkaTemplate,
                            LedgerMetrics metrics,
                            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("kafkaPublish");
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPending() {
        List<OutboxEntry> batch = outboxRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByIdAsc();
        if (batch.isEmpty()) return;

        for (OutboxEntry entry : batch) {
            try {
                circuitBreaker.executeCallable(() -> {
                    publish(entry);
                    return null;
                });
                entry.markPublished();
                metrics.recordOutboxPublish();
            } catch (CallNotPermittedException e) {
                // circuit is OPEN — Kafka is unhealthy; stop this batch entirely,
                // the backlog metric will show it and the next poll tries again later
                metrics.setOutboxBacklog(outboxRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByIdAsc().size());
                return;
            } catch (Exception e) {
                entry.incrementRetryCount();
                if (entry.getRetryCount() >= MAX_RETRIES) {
                    entry.markDeadLettered();
                    metrics.recordOutboxDeadLettered();
                }
            }
        }

        outboxRepository.saveAll(batch);
        metrics.setOutboxBacklog(outboxRepository.findTop100ByPublishedFalseAndDeadLetteredFalseOrderByIdAsc().size());
    }

    private void publish(OutboxEntry entry) {
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                entry.getAggregateId(), entry.getSequenceNumber(),
                entry.getEventType(), entry.getPayload());
        kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, entry.getAggregateId(), envelope).get(); // .get() makes it synchronous for the circuit breaker to see failures
    }
}