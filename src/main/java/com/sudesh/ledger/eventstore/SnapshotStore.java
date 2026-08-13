package com.sudesh.ledger.eventstore;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class SnapshotStore {

    // every 20 events, take a new snapshot — small enough to demo, real
    // systems tune this based on actual event size / replay cost
    private static final int SNAPSHOT_FREQUENCY = 20;

    private final AccountSnapshotRepository repository;

    public SnapshotStore(AccountSnapshotRepository repository) {
        this.repository = repository;
    }

    public Optional<AccountSnapshot> loadLatest(String aggregateId) {
        return repository.findByAggregateId(aggregateId);
    }

    @Transactional
    public void saveIfDue(String aggregateId, long currentVersion, AccountSnapshot candidate) {
        long lastSnapshotVersion = repository.findByAggregateId(aggregateId)
                .map(AccountSnapshot::getVersion)
                .orElse(0L);

        if (currentVersion - lastSnapshotVersion >= SNAPSHOT_FREQUENCY) {
            repository.save(candidate); // overwrites — @Id is aggregateId
        }
    }
}