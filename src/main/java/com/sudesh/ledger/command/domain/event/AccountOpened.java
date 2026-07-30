package com.sudesh.ledger.command.domain.event;

public record AccountOpened(String accountId, String ownerName, java.math.BigDecimal openingBalance) {
  
}