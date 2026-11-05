package com.adamtts.studio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FfmpegNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFfmpegNotFound(FfmpegNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "FFMPEG_NOT_FOUND", "message", ex.getMessage()));
    }

    @ExceptionHandler(NoAvailableKeyException.class)
    public ResponseEntity<Map<String, Object>> handleNoAvailableKey(NoAvailableKeyException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of("error", "NO_KEYS_AVAILABLE", "message", ex.getMessage()));
    }

    @ExceptionHandler(ElevenLabsApiException.class)
    public ResponseEntity<Map<String, Object>> handleElevenLabsApi(ElevenLabsApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of(
                        "error", "ELEVENLABS_ERROR",
                        "statusCode", ex.getStatusCode(),
                        "message", ex.getMessage(),
                        "rawResponse", ex.getRawResponseBody()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "VALIDATION_FAILED", "message", message != null ? message : "Dữ liệu không hợp lệ"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "BAD_REQUEST", "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", "Đã xảy ra lỗi hệ thống: " + ex.getMessage()));
    }
}

