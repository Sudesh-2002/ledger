package com.sudesh.ledger.command.domain.command;

public record DepositCommand(String accountId, java.math.BigDecimal amount, String reference) {
  
}