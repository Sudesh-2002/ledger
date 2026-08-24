package com.sudesh.ledger.eventstore;

import com.sudesh.ledger.shared.event.DomainEventEnvelope;
import com.sudesh.ledger.shared.metrics.LedgerMetrics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.sudesh.ledger.config.KafkaTopicConfig.ACCOUNT_EVENTS_TOPIC;

@Component
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, DomainEventEnvelope> kafkaTemplate;
    private final LedgerMetrics metrics;

    public OutboxPublisher(OutboxRepository outboxRepository,
                            KafkaTemplate<String, DomainEventEnvelope> kafkaTemplate,
                            LedgerMetrics metrics) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.metrics = metrics;
    }

    @Scheduled(fixedDelay = 500) // poll twice a second — tune based on acceptable publish latency
    @Transactional
    public void publishPending() {
        List<OutboxEntry> batch = outboxRepository.findTop100ByPublishedFalseOrderByIdAsc();
        if (batch.isEmpty()) return;

        for (OutboxEntry entry : batch) {
            DomainEventEnvelope envelope = new DomainEventEnvelope(
                    entry.getAggregateId(), entry.getSequenceNumber(),
                    entry.getEventType(), entry.getPayload());

            // synchronous send — simpler to reason about; the batch is small,
            // and a stuck publish here is exactly what backlog metrics should surface
            kafkaTemplate.send(ACCOUNT_EVENTS_TOPIC, entry.getAggregateId(), envelope);
            entry.markPublished();
            metrics.recordOutboxPublish();
        }

        outboxRepository.saveAll(batch);
        metrics.setOutboxBacklog(outboxRepository.findTop100ByPublishedFalseOrderByIdAsc().size());
    }
}