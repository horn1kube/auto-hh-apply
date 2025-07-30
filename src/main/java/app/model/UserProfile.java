package app.model;

import java.util.List;
import java.util.Map;

/**
 * Модель профиля пользователя с предпочтениями и навыками
 */
public class UserProfile {
    private String fullName;
    private String summary;
    private List<String> skills;
    private int experienceYears;
    private List<String> stack;
    private List<String> preferredLocations;
    private String preferredWorkFormat; // remote/office/hybrid
    private SalaryExpectations salaryExpectations;
    private Map<String, String> languages; // язык -> уровень
    private Map<String, String> extras; // дополнительные данные

    // Вложенный класс для зарплатных ожиданий
    public static class SalaryExpectations {
        private int min;
        private int max;
        private int current;

        public SalaryExpectations() {}

        public SalaryExpectations(int min, int max, int current) {
            this.min = min;
            this.max = max;
            this.current = current;
        }

        public int getMin() { return min; }
        public void setMin(int min) { this.min = min; }

        public int getMax() { return max; }
        public void setMax(int max) { this.max = max; }

        public int getCurrent() { return current; }
        public void setCurrent(int current) { this.current = current; }

        @Override
        public String toString() {
            return String.format("SalaryExpectations{min=%d, max=%d, current=%d}", min, max, current);
        }
    }

    // Конструкторы
    public UserProfile() {}

    public UserProfile(String fullName, String summary, List<String> skills, 
                      int experienceYears, List<String> stack, List<String> preferredLocations,
                      String preferredWorkFormat, SalaryExpectations salaryExpectations,
                      Map<String, String> languages, Map<String, String> extras) {
        this.fullName = fullName;
        this.summary = summary;
        this.skills = skills;
        this.experienceYears = experienceYears;
        this.stack = stack;
        this.preferredLocations = preferredLocations;
        this.preferredWorkFormat = preferredWorkFormat;
        this.salaryExpectations = salaryExpectations;
        this.languages = languages;
        this.extras = extras;
    }

    // Геттеры и сеттеры
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public int getExperienceYears() { return experienceYears; }
    public void setExperienceYears(int experienceYears) { this.experienceYears = experienceYears; }

    public List<String> getStack() { return stack; }
    public void setStack(List<String> stack) { this.stack = stack; }

    public List<String> getPreferredLocations() { return preferredLocations; }
    public void setPreferredLocations(List<String> preferredLocations) { this.preferredLocations = preferredLocations; }

    public String getPreferredWorkFormat() { return preferredWorkFormat; }
    public void setPreferredWorkFormat(String preferredWorkFormat) { this.preferredWorkFormat = preferredWorkFormat; }

    public SalaryExpectations getSalaryExpectations() { return salaryExpectations; }
    public void setSalaryExpectations(SalaryExpectations salaryExpectations) { this.salaryExpectations = salaryExpectations; }

    public Map<String, String> getLanguages() { return languages; }
    public void setLanguages(Map<String, String> languages) { this.languages = languages; }

    public Map<String, String> getExtras() { return extras; }
    public void setExtras(Map<String, String> extras) { this.extras = extras; }

    @Override
    public String toString() {
        return "UserProfile{" +
                "fullName='" + fullName + '\'' +
                ", summary='" + summary + '\'' +
                ", skills=" + skills +
                ", experienceYears=" + experienceYears +
                ", stack=" + stack +
                ", preferredLocations=" + preferredLocations +
                ", preferredWorkFormat='" + preferredWorkFormat + '\'' +
                ", salaryExpectations=" + salaryExpectations +
                ", languages=" + languages +
                ", extras=" + extras +
                '}';
    }
} 