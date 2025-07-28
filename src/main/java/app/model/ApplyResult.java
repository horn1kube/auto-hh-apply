package app.model;

import java.time.LocalDateTime;

public record ApplyResult(
    String vacancyId,
    boolean success,
    int statusCode,
    String message,
    LocalDateTime timestamp
) {
    public static ApplyResult success(String vacancyId) {
        return new ApplyResult(vacancyId, true, 200, "OK", LocalDateTime.now());
    }
    
    public static ApplyResult failure(String vacancyId, int statusCode, String message) {
        return new ApplyResult(vacancyId, false, statusCode, message, LocalDateTime.now());
    }
} 