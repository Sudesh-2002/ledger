package com.sudesh.ledger.query.api.dto;

import java.math.BigDecimal;

public record AccountSummaryResponse(String accountId, String ownerName, BigDecimal balance, String status) {}