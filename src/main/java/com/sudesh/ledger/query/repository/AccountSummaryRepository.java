package com.sudesh.ledger.query.repository;

import com.sudesh.ledger.query.projection.AccountSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountSummaryRepository extends JpaRepository<AccountSummaryProjection, String> {}