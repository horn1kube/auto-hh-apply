package app.cli;

import app.model.EvaluationResult;
import app.model.UserProfile;
import app.service.OllamaClient;
import app.service.VacancyEvaluationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Простой CLI для тестирования оценки вакансий
 */
public class EvaluationCli {
    private static final Logger log = LoggerFactory.getLogger(EvaluationCli.class);
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Использование: java EvaluationCli <vacancyId> <userProfileFile>");
            System.out.println("Пример: java EvaluationCli 12345678 examples/user.sample.json");
            System.exit(1);
        }
        
        String vacancyId = args[0];
        String userProfileFile = args[1];
        
        try {
            // Загружаем профиль пользователя
            String userProfileJson = Files.readString(Paths.get(userProfileFile));
            ObjectMapper objectMapper = new ObjectMapper();
            UserProfile userProfile = objectMapper.readValue(userProfileJson, UserProfile.class);
            
            System.out.println("=== Оценка вакансии ===");
            System.out.println("ID вакансии: " + vacancyId);
            System.out.println("Пользователь: " + userProfile.getFullName());
            System.out.println();
            
            // Создаем сервис (для простоты без Spring контекста)
            OllamaClient ollamaClient = new OllamaClient();
            VacancyEvaluationService evaluationService = new VacancyEvaluationService(null, null, ollamaClient);
            
            // Оцениваем вакансию
            EvaluationResult result = evaluationService.evaluateVacancy(vacancyId, userProfile);
            
            // Выводим результат
            printResult(result);
            
        } catch (Exception e) {
            log.error("Ошибка при оценке вакансии: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
    
    private static void printResult(EvaluationResult result) {
        System.out.println("=== РЕЗУЛЬТАТЫ ОЦЕНКИ ===");
        System.out.println();
        
        // Вакансия → Кандидат
        System.out.println("📊 Вакансия → Кандидат: " + result.getJobToUserFit().getScore() + "/100");
        System.out.println("Причины:");
        for (String reason : result.getJobToUserFit().getReasons()) {
            System.out.println("  • " + reason);
        }
        System.out.println();
        
        // Кандидат → Вакансия
        System.out.println("📊 Кандидат → Вакансия: " + result.getUserToJobFit().getScore() + "/100");
        System.out.println("Причины:");
        for (String reason : result.getUserToJobFit().getReasons()) {
            System.out.println("  • " + reason);
        }
        
        if (result.getUserToJobFit().getMissingSkills() != null && !result.getUserToJobFit().getMissingSkills().isEmpty()) {
            System.out.println("Отсутствующие навыки:");
            for (String skill : result.getUserToJobFit().getMissingSkills()) {
                System.out.println("  • " + skill);
            }
        }
        System.out.println();
        
        // Рекомендация
        System.out.println("🎯 РЕКОМЕНДАЦИЯ: " + result.getSuggestion());
        System.out.println("Уверенность: " + String.format("%.1f%%", result.getConfidence() * 100));
        System.out.println();
        
        if ("Apply".equals(result.getSuggestion())) {
            System.out.println("✅ РЕКОМЕНДУЕТСЯ ОТКЛИК");
        } else {
            System.out.println("❌ НЕ РЕКОМЕНДУЕТСЯ");
        }
    }
} 