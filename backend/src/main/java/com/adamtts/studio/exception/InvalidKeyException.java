package com.adamtts.studio.exception;

public class InvalidKeyException extends RuntimeException {
    private final String keyId;

    public InvalidKeyException(String keyId, String message) {
        super(message);
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }
}

