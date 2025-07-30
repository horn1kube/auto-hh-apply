package app.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Утилиты для сравнения навыков пользователя и вакансии
 */
public class SkillMatcher {
    
    /**
     * Находит пересекающиеся навыки между пользователем и вакансией
     */
    public static List<String> findOverlappingSkills(List<String> userSkills, List<String> vacancySkills) {
        if (userSkills == null || vacancySkills == null) {
            return List.of();
        }
        
        List<String> normalizedUserSkills = normalizeSkills(userSkills);
        List<String> normalizedVacancySkills = normalizeSkills(vacancySkills);
        
        return normalizedUserSkills.stream()
                .filter(normalizedVacancySkills::contains)
                .collect(Collectors.toList());
    }
    
    /**
     * Находит навыки, которых не хватает у пользователя
     */
    public static List<String> findMissingSkills(List<String> userSkills, List<String> vacancySkills) {
        if (userSkills == null || vacancySkills == null) {
            return vacancySkills != null ? normalizeSkills(vacancySkills) : List.of();
        }
        
        List<String> normalizedUserSkills = normalizeSkills(userSkills);
        List<String> normalizedVacancySkills = normalizeSkills(vacancySkills);
        
        return normalizedVacancySkills.stream()
                .filter(skill -> !normalizedUserSkills.contains(skill))
                .collect(Collectors.toList());
    }
    
    /**
     * Вычисляет процент покрытия навыков (сколько навыков из вакансии есть у пользователя)
     */
    public static double calculateSkillCoverage(List<String> userSkills, List<String> vacancySkills) {
        if (vacancySkills == null || vacancySkills.isEmpty()) {
            return 1.0; // Если навыков нет, считаем 100% покрытие
        }
        
        List<String> overlapping = findOverlappingSkills(userSkills, vacancySkills);
        return (double) overlapping.size() / vacancySkills.size();
    }
    
    /**
     * Нормализует список навыков: приводит к нижнему регистру и убирает лишние пробелы
     */
    public static List<String> normalizeSkills(List<String> skills) {
        if (skills == null) {
            return List.of();
        }
        
        return skills.stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(skill -> !skill.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Вычисляет общий скор навыков (0-100)
     */
    public static int calculateSkillScore(List<String> userSkills, List<String> vacancySkills) {
        if (vacancySkills == null || vacancySkills.isEmpty()) {
            return 100; // Если навыков нет, считаем максимальный скор
        }
        
        double coverage = calculateSkillCoverage(userSkills, vacancySkills);
        return (int) (coverage * 100);
    }
    
    /**
     * Проверяет, есть ли критически важные навыки, которых не хватает
     */
    public static boolean hasCriticalMissingSkills(List<String> userSkills, List<String> vacancySkills) {
        List<String> missingSkills = findMissingSkills(userSkills, vacancySkills);
        
        // Список критически важных навыков (можно расширить)
        List<String> criticalSkills = List.of(
                "java", "spring", "python", "javascript", "react", "angular", "vue",
                "sql", "postgresql", "mysql", "mongodb", "redis", "docker", "kubernetes",
                "git", "maven", "gradle", "jenkins", "junit", "mockito"
        );
        
        return missingSkills.stream()
                .anyMatch(skill -> criticalSkills.contains(skill.toLowerCase()));
    }
} 