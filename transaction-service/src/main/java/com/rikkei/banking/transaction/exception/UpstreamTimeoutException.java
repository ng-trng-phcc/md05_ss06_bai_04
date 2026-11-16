package com.rikkei.banking.transaction.exception;

public class UpstreamTimeoutException extends RuntimeException {
    public UpstreamTimeoutException(String message) {
        super(message);
    }
}
