package com.rikkei.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank(message = "accountNumber is required")
        @Size(max = 50, message = "accountNumber must be at most 50 characters")
        String accountNumber,

        @NotBlank(message = "ownerName is required")
        @Size(max = 255, message = "ownerName must be at most 255 characters")
        String ownerName,

        @NotNull(message = "initialBalance is required")
        @DecimalMin(value = "0.00", message = "initialBalance cannot be negative")
        @Digits(integer = 17, fraction = 2, message = "initialBalance must have at most 17 integer digits and 2 decimal places")
        BigDecimal initialBalance
) {
}
