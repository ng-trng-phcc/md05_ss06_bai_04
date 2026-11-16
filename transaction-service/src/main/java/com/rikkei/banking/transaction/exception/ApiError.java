package com.rikkei.banking.transaction.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId
) {
    public static ApiError of(int status, String error, String message, String path, String requestId) {
        return new ApiError(Instant.now(), status, error, message, path, requestId);
    }
}
