package com.rikkei.banking.transaction.exception;

public class UpstreamServiceUnavailableException extends RuntimeException {
    public UpstreamServiceUnavailableException(String message) {
        super(message);
    }
}
