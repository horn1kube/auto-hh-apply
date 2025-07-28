package app.model;

import java.time.LocalDateTime;

public record ApplyLog(
    String vacancyId,
    String action,
    String message,
    LocalDateTime timestamp
) {
    public static ApplyLog found(String vacancyId) {
        return new ApplyLog(vacancyId, "FOUND", "Found vacancy", LocalDateTime.now());
    }
    
    public static ApplyLog skip(String vacancyId, String reason) {
        return new ApplyLog(vacancyId, "SKIP", reason, LocalDateTime.now());
    }
    
    public static ApplyLog applyOk(String vacancyId) {
        return new ApplyLog(vacancyId, "APPLY_OK", "Successfully applied", LocalDateTime.now());
    }
    
    public static ApplyLog applyFail(String vacancyId, String reason) {
        return new ApplyLog(vacancyId, "APPLY_FAIL", reason, LocalDateTime.now());
    }
} 