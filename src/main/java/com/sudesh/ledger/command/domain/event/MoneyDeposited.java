package com.sudesh.ledger.command.domain.event;

public record MoneyDeposited(String accountId, java.math.BigDecimal amount, String reference) {
  
}