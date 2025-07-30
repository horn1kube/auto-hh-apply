package app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Результат оценки соответствия вакансии и пользователя
 */
public class EvaluationResult {
    
    public static class FitScore {
        private int score;
        private List<String> reasons;

        public FitScore() {}

        public FitScore(int score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> reasons) { this.reasons = reasons; }
    }

    public static class UserToJobFit extends FitScore {
        private List<String> missingSkills;

        public UserToJobFit() {}

        public UserToJobFit(int score, List<String> reasons, List<String> missingSkills) {
            super(score, reasons);
            this.missingSkills = missingSkills;
        }

        public List<String> getMissingSkills() { return missingSkills; }
        public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }
    }

    @JsonProperty("jobToUserFit")
    private FitScore jobToUserFit;
    
    @JsonProperty("userToJobFit")
    private UserToJobFit userToJobFit;
    
    private String suggestion; // "Apply" или "Skip"
    private double confidence; // 0.0-1.0
    private JsonNode rawModelJson;

    // Конструкторы
    public EvaluationResult() {}

    public EvaluationResult(FitScore jobToUserFit, UserToJobFit userToJobFit, 
                           String suggestion, double confidence, JsonNode rawModelJson) {
        this.jobToUserFit = jobToUserFit;
        this.userToJobFit = userToJobFit;
        this.suggestion = suggestion;
        this.confidence = confidence;
        this.rawModelJson = rawModelJson;
    }

    // Геттеры и сеттеры
    public FitScore getJobToUserFit() { return jobToUserFit; }
    public void setJobToUserFit(FitScore jobToUserFit) { this.jobToUserFit = jobToUserFit; }

    public UserToJobFit getUserToJobFit() { return userToJobFit; }
    public void setUserToJobFit(UserToJobFit userToJobFit) { this.userToJobFit = userToJobFit; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public JsonNode getRawModelJson() { return rawModelJson; }
    public void setRawModelJson(JsonNode rawModelJson) { this.rawModelJson = rawModelJson; }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "jobToUserFit=" + jobToUserFit +
                ", userToJobFit=" + userToJobFit +
                ", suggestion='" + suggestion + '\'' +
                ", confidence=" + confidence +
                '}';
    }
} 