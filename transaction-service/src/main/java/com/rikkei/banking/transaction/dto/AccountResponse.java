package com.rikkei.banking.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        Instant createdAt
) {
}
