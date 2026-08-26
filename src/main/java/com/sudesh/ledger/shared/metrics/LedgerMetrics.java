package com.sudesh.ledger.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class LedgerMetrics {

    private final Counter commandsProcessed;
    private final Counter commandsRejected;
    private final Counter outboxPublished;
    private final Counter outboxDeadLettered;
    private final AtomicInteger outboxBacklog = new AtomicInteger(0);
    private final Timer commandLatency;

    public LedgerMetrics(MeterRegistry registry) {
        this.commandsProcessed = Counter.builder("ledger.commands.processed")
                .description("Total commands successfully processed").register(registry);
        this.commandsRejected = Counter.builder("ledger.commands.rejected")
                .description("Total commands rejected by domain rules").register(registry);
        this.outboxPublished = Counter.builder("ledger.outbox.published")
                .description("Total outbox entries published to Kafka").register(registry);
        this.outboxDeadLettered = Counter.builder("ledger.outbox.dead_lettered")
                .description("Outbox entries that exhausted retries and were dead-lettered").register(registry);
        this.commandLatency = Timer.builder("ledger.command.latency")
                .description("Time to process a command end-to-end").register(registry);
        registry.gauge("ledger.outbox.backlog", outboxBacklog);
    }

    public void recordCommandProcessed() { commandsProcessed.increment(); }
    public void recordCommandRejected() { commandsRejected.increment(); }
    public void recordOutboxPublish() { outboxPublished.increment(); }
    public void recordOutboxDeadLettered() { outboxDeadLettered.increment(); }
    public void setOutboxBacklog(int size) { outboxBacklog.set(size); }
    public Timer.Sample startCommandTimer() { return Timer.start(); }
    public void stopCommandTimer(Timer.Sample sample) { sample.stop(commandLatency); }
}