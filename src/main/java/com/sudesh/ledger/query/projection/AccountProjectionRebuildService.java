package com.sudesh.ledger.query.projection;

import com.sudesh.ledger.command.domain.AccountEventCodec;
import com.sudesh.ledger.eventstore.EventStoreRepository;
import com.sudesh.ledger.eventstore.StoredEvent;
import com.sudesh.ledger.query.repository.AccountSummaryRepository;
import com.sudesh.ledger.query.repository.TransactionHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountProjectionRebuildService {

    private final EventStoreRepository eventStoreRepository;
    private final AccountEventCodec codec;
    private final AccountProjector projector;
    private final AccountSummaryRepository summaryRepository;
    private final TransactionHistoryRepository historyRepository;

    public AccountProjectionRebuildService(EventStoreRepository eventStoreRepository,
                                            AccountEventCodec codec,
                                            AccountProjector projector,
                                            AccountSummaryRepository summaryRepository,
                                            TransactionHistoryRepository historyRepository) {
        this.eventStoreRepository = eventStoreRepository;
        this.codec = codec;
        this.projector = projector;
        this.summaryRepository = summaryRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void rebuildAll() {
        historyRepository.deleteAll();
        summaryRepository.deleteAll();

        for (StoredEvent stored : eventStoreRepository.findAllByOrderByAggregateIdAscSequenceNumberAsc()) {
            Object domainEvent = codec.toDomainEvent(stored);
            projector.apply(new AccountProjectorEnvelope(
                    stored.getAggregateId(), stored.getSequenceNumber(), domainEvent));
        }
    }
}