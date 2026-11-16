package com.rikkei.banking.transaction.exception;

public class SourceAccountNotFoundException extends RuntimeException {
    public SourceAccountNotFoundException(Long id) {
        super("Source account not found: " + id);
    }
}
