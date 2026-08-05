package com.sudesh.ledger.query.repository;

import com.sudesh.ledger.query.projection.TransactionHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistoryEntry, Long> {
    List<TransactionHistoryEntry> findByAccountIdOrderByOccurredAtAsc(String accountId);
}