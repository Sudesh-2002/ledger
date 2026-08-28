package com.sudesh.ledger.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountSnapshotRepository extends JpaRepository<AccountSnapshot, String> {
    Optional<AccountSnapshot> findByAggregateId(String aggregateId);
}