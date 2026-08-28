package com.sudesh.ledger.query.projection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sudesh.ledger.command.domain.event.AccountOpened;
import com.sudesh.ledger.command.domain.event.MoneyDeposited;
import com.sudesh.ledger.command.domain.event.MoneyWithdrawn;
import com.sudesh.ledger.query.repository.AccountSummaryRepository;
import com.sudesh.ledger.query.repository.TransactionHistoryRepository;
import com.sudesh.ledger.shared.event.DomainEventEnvelope;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountProjector {

    private final AccountSummaryRepository summaryRepository;
    private final TransactionHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    public AccountProjector(AccountSummaryRepository summaryRepository,
                             TransactionHistoryRepository historyRepository,
                             ObjectMapper objectMapper) {
        this.summaryRepository = summaryRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    /** Called by Spring's in-process event bus (kept for flexibility). */
    @EventListener
    @Transactional
    public void on(DomainEventEnvelope envelope) {
        try {
            Class<?> type = switch (envelope.eventType()) {
                case "AccountOpened" -> AccountOpened.class;
                case "MoneyDeposited" -> MoneyDeposited.class;
                case "MoneyWithdrawn" -> MoneyWithdrawn.class;
                default -> null;
            };
            if (type == null) return;
            Object domainEvent = objectMapper.readValue(envelope.payloadJson(), type);
            apply(new AccountProjectorEnvelope(envelope.aggregateId(), envelope.sequenceNumber(), domainEvent));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize event in @EventListener", e);
        }
    }

    // Shared by both the live Kafka consumer and the rebuild service
    void apply(AccountProjectorEnvelope envelope) {
        switch (envelope.payload()) {
            case AccountOpened e -> handleOpened(envelope, e);
            case MoneyDeposited e -> handleDeposit(envelope, e);
            case MoneyWithdrawn e -> handleWithdraw(envelope, e);
            default -> { /* ignore unknown event types */ }
        }
    }

    private void handleOpened(AccountProjectorEnvelope envelope, AccountOpened e) {
        if (summaryRepository.existsById(e.accountId())) return; // idempotent

        summaryRepository.save(new AccountSummaryProjection(
                e.accountId(), e.ownerName(), e.openingBalance(), "OPEN", envelope.sequenceNumber()));

        historyRepository.save(new TransactionHistoryEntry(
                e.accountId(), "OPEN", e.openingBalance(), "account-opened", e.openingBalance()));
    }

    private void handleDeposit(AccountProjectorEnvelope envelope, MoneyDeposited e) {
        AccountSummaryProjection summary = summaryRepository.findById(e.accountId())
                .orElseThrow(() -> new IllegalStateException("Projection missing for " + e.accountId()));
        if (summary.getVersion() >= envelope.sequenceNumber()) return; // already applied

        summary.setBalance(summary.getBalance().add(e.amount()));
        summary.setVersion(envelope.sequenceNumber());
        summaryRepository.save(summary);

        historyRepository.save(new TransactionHistoryEntry(
                e.accountId(), "DEPOSIT", e.amount(), e.reference(), summary.getBalance()));
    }

    private void handleWithdraw(AccountProjectorEnvelope envelope, MoneyWithdrawn e) {
        AccountSummaryProjection summary = summaryRepository.findById(e.accountId())
                .orElseThrow(() -> new IllegalStateException("Projection missing for " + e.accountId()));
        if (summary.getVersion() >= envelope.sequenceNumber()) return;

        summary.setBalance(summary.getBalance().subtract(e.amount()));
        summary.setVersion(envelope.sequenceNumber());
        summaryRepository.save(summary);

        historyRepository.save(new TransactionHistoryEntry(
                e.accountId(), "WITHDRAWAL", e.amount(), e.reference(), summary.getBalance()));
    }
}