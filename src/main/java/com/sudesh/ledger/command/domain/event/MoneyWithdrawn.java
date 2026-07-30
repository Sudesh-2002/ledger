package com.sudesh.ledger.command.domain.event;

public record MoneyWithdrawn(String accountId, java.math.BigDecimal amount, String reference) {
  
}