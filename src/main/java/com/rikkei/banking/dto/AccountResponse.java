package com.rikkei.banking.dto;

import com.rikkei.banking.entity.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String accountNumber,
        String ownerName,
        BigDecimal balance,
        Instant createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getOwnerName(),
                account.getBalance(),
                account.getCreatedAt()
        );
    }
}
