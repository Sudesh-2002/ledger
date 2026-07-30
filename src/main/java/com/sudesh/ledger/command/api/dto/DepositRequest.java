package com.sudesh.ledger.command.api.dto;

import java.math.BigDecimal;

public record DepositRequest(BigDecimal amount, String reference) {
  
}