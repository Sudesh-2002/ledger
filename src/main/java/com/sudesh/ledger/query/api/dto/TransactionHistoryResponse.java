package com.sudesh.ledger.query.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryResponse(String type, BigDecimal amount, String reference,
                                          BigDecimal balanceAfter, Instant occurredAt) {}