package com.sudesh.ledger.command.domain.command;

public record WithdrawCommand(String accountId, java.math.BigDecimal amount, String reference) {
  
}