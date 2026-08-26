package com.sudesh.ledger.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEntry, Long> {
    List<OutboxEntry> findTop100ByPublishedFalseAndDeadLetteredFalseOrderByIdAsc();
}