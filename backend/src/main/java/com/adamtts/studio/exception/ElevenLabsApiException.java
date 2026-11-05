package com.adamtts.studio.exception;

public class ElevenLabsApiException extends RuntimeException {
    private final int statusCode;
    private final String rawResponseBody;

    public ElevenLabsApiException(int statusCode, String rawResponseBody) {
        super("Lỗi từ ElevenLabs (HTTP " + statusCode + "): " + rawResponseBody);
        this.statusCode = statusCode;
        this.rawResponseBody = rawResponseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }
}

