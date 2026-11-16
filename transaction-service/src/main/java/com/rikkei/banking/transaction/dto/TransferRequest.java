package com.rikkei.banking.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "sourceAccountId is required")
        @Positive(message = "sourceAccountId must be positive")
        Long sourceAccountId,

        @NotNull(message = "destinationAccountId is required")
        @Positive(message = "destinationAccountId must be positive")
        Long destinationAccountId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than zero")
        @Digits(integer = 17, fraction = 2, message = "amount must have at most 17 integer digits and 2 decimal places")
        BigDecimal amount
) {
}
