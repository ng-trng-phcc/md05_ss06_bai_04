package com.rikkei.banking.transaction.exception;

public class UpstreamFailureException extends RuntimeException {
    public UpstreamFailureException(String message) {
        super(message);
    }
}
