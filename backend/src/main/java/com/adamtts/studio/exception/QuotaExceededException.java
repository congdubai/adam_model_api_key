package com.adamtts.studio.exception;

public class QuotaExceededException extends RuntimeException {
    private final String keyId;

    public QuotaExceededException(String keyId, String message) {
        super(message);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }
}

