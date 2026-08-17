package com.sudesh.ledger.command.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OpenAccountRequest(
        @NotBlank String accountId,
        @NotBlank String ownerName,
        @NotNull @DecimalMin(value = "0.00") BigDecimal openingBalance
) {}