package app.model;

import java.time.LocalDateTime;

public record ApplySummary(
    LocalDateTime startTime,
    LocalDateTime endTime,
    int totalFound,
    int newVacancies,
    int applied,
    int errors,
    boolean dryRun
) {
    public static ApplySummary create(LocalDateTime startTime, int totalFound, int newVacancies, int applied, int errors, boolean dryRun) {
        return new ApplySummary(startTime, LocalDateTime.now(), totalFound, newVacancies, applied, errors, dryRun);
    }
} 