package com.rikkei.banking.transaction.dto;

import java.math.BigDecimal;

public record TransferResponse(
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String status,
        String message
) {
}
