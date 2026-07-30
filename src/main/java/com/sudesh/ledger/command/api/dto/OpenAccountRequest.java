package com.sudesh.ledger.command.api.dto;

import java.math.BigDecimal;

public record OpenAccountRequest(String accountId, String ownerName, BigDecimal openingBalance) {
  
}