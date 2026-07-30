package com.sudesh.ledger.command.api.dto;

import java.math.BigDecimal;

public record WithdrawRequest(BigDecimal amount, String reference) {
  
}