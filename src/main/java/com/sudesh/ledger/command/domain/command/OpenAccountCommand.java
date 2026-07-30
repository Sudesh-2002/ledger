package com.sudesh.ledger.command.domain.command;

public record OpenAccountCommand(String accountId, String ownerName, java.math.BigDecimal openingBalance) {
  
}