package app.service;

import app.config.Env;
import app.hh.HhClient;
import app.model.EvaluationResult;
import app.model.UserProfile;
import app.model.Vacancy;
import app.util.SkillMatcher;
import app.util.TextUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис для оценки соответствия вакансий и пользователей
 */
@Service
public class VacancyEvaluationService {
    private static final Logger log = LoggerFactory.getLogger(VacancyEvaluationService.class);
    
    private final Env env;
    private final HhClient hhClient;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    private WebDriver driver;
    
    public VacancyEvaluationService(Env env, HhClient hhClient, OllamaClient ollamaClient) {
        this.env = env;
        this.hhClient = hhClient;
        this.ollamaClient = ollamaClient;
        this.objectMapper = new ObjectMapper();
        initializeDriver();
    }
    
    /**
     * Оценивает соответствие вакансии и пользователя
     */
    public EvaluationResult evaluateVacancy(String vacancyId, UserProfile userProfile) throws Exception {
        log.info("Начинаем оценку вакансии {} для пользователя {}", vacancyId, userProfile.getFullName());
        
        // Получаем данные вакансии
        Vacancy vacancy = fetchVacancyById(vacancyId);
        if (vacancy == null) {
            throw new RuntimeException("Не удалось получить данные вакансии: " + vacancyId);
        }
        
        // Проверяем доступность Ollama
        if (!ollamaClient.isAvailable()) {
            log.warn("Ollama недоступна, используем эвристическую оценку");
            return evaluateWithHeuristics(vacancy, userProfile);
        }
        
        // Строим промпт для LLM
        String prompt = buildPrompt(vacancy, userProfile);
        
        // Вызываем Ollama
        String response = ollamaClient.generate(prompt);
        
        // Парсим и валидируем результат
        return parseAndValidateResult(response, vacancy, userProfile);
    }
    
    /**
     * Инициализирует Chrome драйвер
     */
    private void initializeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Запуск в фоновом режиме
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        this.driver = new ChromeDriver(options);
        this.driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
    }
    
    /**
     * Получает данные вакансии по ID
     */
    private Vacancy fetchVacancyById(String vacancyId) throws Exception {
        String url = "https://hh.ru/vacancy/" + vacancyId;
        log.info("Загружаем вакансию: {}", url);
        
        try {
            driver.get(url);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("h1")));
            Thread.sleep(2000); // Даем время для загрузки динамического контента
            
            return parseVacancy(vacancyId, url);
        } catch (Exception e) {
            log.error("Ошибка при загрузке вакансии {}: {}", vacancyId, e.getMessage());
            return null;
        }
    }
    
    /**
     * Парсит страницу вакансии с помощью Selenium
     */
    private Vacancy parseVacancy(String vacancyId, String sourceUrl) {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(vacancyId);
        vacancy.setSourceUrl(sourceUrl);
        
        try {
            // Заголовок вакансии
            WebElement titleElement = driver.findElement(By.cssSelector("h1"));
            if (titleElement != null) {
                vacancy.setTitle(titleElement.getText().trim());
            }
            
            // Компания
            try {
                WebElement companyElement = driver.findElement(By.cssSelector("[data-qa='vacancy-company-name']"));
                if (companyElement != null) {
                    vacancy.setCompany(companyElement.getText().trim());
                }
            } catch (Exception e) {
                log.debug("Не удалось найти название компании: {}", e.getMessage());
            }
            
            // Локация
            try {
                WebElement locationElement = driver.findElement(By.cssSelector("[data-qa='vacancy-view-location']"));
                if (locationElement != null) {
                    vacancy.setLocation(locationElement.getText().trim());
                }
            } catch (Exception e) {
                log.debug("Не удалось найти локацию: {}", e.getMessage());
            }
            
            // Зарплата
            try {
                WebElement salaryElement = driver.findElement(By.cssSelector("[data-qa='vacancy-salary']"));
                if (salaryElement != null) {
                    vacancy.setSalaryRaw(salaryElement.getText().trim());
                }
            } catch (Exception e) {
                log.debug("Не удалось найти зарплату: {}", e.getMessage());
            }
            
            // Описание
            try {
                WebElement descriptionElement = driver.findElement(By.cssSelector("[data-qa='vacancy-description']"));
                if (descriptionElement != null) {
                    String description = descriptionElement.getText();
                    vacancy.setDescription(description);
                    
                    // Извлекаем навыки из описания
                    List<String> skills = TextUtils.extractSkills(description);
                    vacancy.setSkills(skills);
                }
            } catch (Exception e) {
                log.debug("Не удалось найти описание: {}", e.getMessage());
            }
            
            // Тип занятости
            try {
                WebElement employmentElement = driver.findElement(By.cssSelector("[data-qa='vacancy-view-employment-mode']"));
                if (employmentElement != null) {
                    vacancy.setEmploymentType(employmentElement.getText().trim());
                }
            } catch (Exception e) {
                log.debug("Не удалось найти тип занятости: {}", e.getMessage());
            }
            
            // Формат работы
            try {
                WebElement workFormatElement = driver.findElement(By.cssSelector("[data-qa='vacancy-view-work-schedule']"));
                if (workFormatElement != null) {
                    vacancy.setWorkFormat(workFormatElement.getText().trim());
                }
            } catch (Exception e) {
                log.debug("Не удалось найти формат работы: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге вакансии: {}", e.getMessage());
        }
        
        return vacancy;
    }
    

    
    /**
     * Строит промпт для LLM
     */
    private String buildPrompt(Vacancy vacancy, UserProfile userProfile) {
        // Ограничиваем длину описания
        String truncatedDescription = TextUtils.truncate(vacancy.getDescription(), 4000);
        
        return String.format("""
            Ты - эксперт по оценке соответствия вакансий и кандидатов. Оцени вакансию и кандидата.
            
            ВАКАНСИЯ:
            - Название: %s
            - Компания: %s
            - Локация: %s
            - Зарплата: %s
            - Навыки: %s
            - Описание: %s
            - Тип занятости: %s
            - Формат работы: %s
            
            КАНДИДАТ:
            - Имя: %s
            - Опыт: %d лет
            - Навыки: %s
            - Стек: %s
            - Предпочитаемые локации: %s
            - Предпочитаемый формат: %s
            - Ожидания по зарплате: %s-%s руб.
            
            Оцени по шкале 0-100:
            1. Насколько вакансия подходит кандидату (jobToUserFit)
            2. Насколько кандидат подходит вакансии (userToJobFit)
            
            Верни строго в JSON формате:
            {
              "jobToUserFit": {
                "score": 85,
                "reasons": ["Подходящие навыки", "Интересная компания"]
              },
              "userToJobFit": {
                "score": 90,
                "reasons": ["Опыт работы", "Знание технологий"],
                "missingSkills": ["Docker"]
              },
              "suggestion": "Apply",
              "confidence": 0.85
            }
            """,
            vacancy.getTitle() != null ? vacancy.getTitle() : "",
            vacancy.getCompany() != null ? vacancy.getCompany() : "",
            vacancy.getLocation() != null ? vacancy.getLocation() : "",
            vacancy.getSalaryRaw() != null ? vacancy.getSalaryRaw() : "",
            vacancy.getSkills() != null ? String.join(", ", vacancy.getSkills()) : "",
            truncatedDescription,
            vacancy.getEmploymentType() != null ? vacancy.getEmploymentType() : "",
            vacancy.getWorkFormat() != null ? vacancy.getWorkFormat() : "",
            userProfile.getFullName() != null ? userProfile.getFullName() : "",
            userProfile.getExperienceYears(),
            userProfile.getSkills() != null ? String.join(", ", userProfile.getSkills()) : "",
            userProfile.getStack() != null ? String.join(", ", userProfile.getStack()) : "",
            userProfile.getPreferredLocations() != null ? String.join(", ", userProfile.getPreferredLocations()) : "",
            userProfile.getPreferredWorkFormat() != null ? userProfile.getPreferredWorkFormat() : "",
            userProfile.getSalaryExpectations() != null ? String.valueOf(userProfile.getSalaryExpectations().getMin()) : "0",
            userProfile.getSalaryExpectations() != null ? String.valueOf(userProfile.getSalaryExpectations().getMax()) : "0"
        );
    }
    

    
    /**
     * Парсит и валидирует результат от LLM
     */
    private EvaluationResult parseAndValidateResult(String jsonResponse, Vacancy vacancy, UserProfile userProfile) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            // Создаем результат
            EvaluationResult result = new EvaluationResult();
            
            // Парсим jobToUserFit
            JsonNode jobToUserFitNode = jsonNode.get("jobToUserFit");
            if (jobToUserFitNode != null) {
                EvaluationResult.FitScore jobToUserFit = new EvaluationResult.FitScore();
                jobToUserFit.setScore(validateScore(jobToUserFitNode.get("score").asInt()));
                jobToUserFit.setReasons(parseReasons(jobToUserFitNode.get("reasons")));
                result.setJobToUserFit(jobToUserFit);
            }
            
            // Парсим userToJobFit
            JsonNode userToJobFitNode = jsonNode.get("userToJobFit");
            if (userToJobFitNode != null) {
                EvaluationResult.UserToJobFit userToJobFit = new EvaluationResult.UserToJobFit();
                userToJobFit.setScore(validateScore(userToJobFitNode.get("score").asInt()));
                userToJobFit.setReasons(parseReasons(userToJobFitNode.get("reasons")));
                
                // Парсим missingSkills
                JsonNode missingSkillsNode = userToJobFitNode.get("missingSkills");
                if (missingSkillsNode != null && missingSkillsNode.isArray()) {
                    List<String> missingSkills = new ArrayList<>();
                    for (JsonNode skillNode : missingSkillsNode) {
                        missingSkills.add(skillNode.asText());
                    }
                    userToJobFit.setMissingSkills(missingSkills);
                }
                
                result.setUserToJobFit(userToJobFit);
            }
            
            // Парсим suggestion
            JsonNode suggestionNode = jsonNode.get("suggestion");
            if (suggestionNode != null) {
                String suggestion = suggestionNode.asText();
                if ("Apply".equalsIgnoreCase(suggestion) || "Skip".equalsIgnoreCase(suggestion)) {
                    result.setSuggestion(suggestion);
                } else {
                    result.setSuggestion("Skip"); // По умолчанию
                }
            }
            
            // Парсим confidence
            JsonNode confidenceNode = jsonNode.get("confidence");
            if (confidenceNode != null) {
                double confidence = confidenceNode.asDouble();
                result.setConfidence(Math.max(0.0, Math.min(1.0, confidence)));
            }
            
            result.setRawModelJson(jsonNode);
            return result;
            
        } catch (Exception e) {
            log.error("Ошибка парсинга результата LLM: {}", e.getMessage());
            // Возвращаем эвристическую оценку в случае ошибки
            return evaluateWithHeuristics(vacancy, userProfile);
        }
    }
    
    /**
     * Валидирует скор (0-100)
     */
    private int validateScore(int score) {
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Парсит список причин
     */
    private List<String> parseReasons(JsonNode reasonsNode) {
        List<String> reasons = new ArrayList<>();
        if (reasonsNode != null && reasonsNode.isArray()) {
            for (JsonNode reasonNode : reasonsNode) {
                reasons.add(reasonNode.asText());
            }
        }
        return reasons;
    }
    
    /**
     * Эвристическая оценка без LLM
     */
    private EvaluationResult evaluateWithHeuristics(Vacancy vacancy, UserProfile userProfile) {
        // Вычисляем скор навыков
        int skillScore = SkillMatcher.calculateSkillScore(userProfile.getSkills(), vacancy.getSkills());
        
        // Простая эвристика
        int jobToUserScore = skillScore;
        int userToJobScore = skillScore;
        
        // Корректируем по локации
        if (userProfile.getPreferredLocations() != null && vacancy.getLocation() != null) {
            String vacancyLocation = vacancy.getLocation().toLowerCase();
            boolean locationMatch = userProfile.getPreferredLocations().stream()
                    .anyMatch(loc -> vacancyLocation.contains(loc.toLowerCase()));
            if (locationMatch) {
                jobToUserScore += 10;
            } else {
                jobToUserScore -= 20;
            }
        }
        
        // Корректируем по формату работы
        if (userProfile.getPreferredWorkFormat() != null && vacancy.getWorkFormat() != null) {
            if (userProfile.getPreferredWorkFormat().equalsIgnoreCase(vacancy.getWorkFormat())) {
                jobToUserScore += 10;
            }
        }
        
        // Ограничиваем скоры
        jobToUserScore = Math.max(0, Math.min(100, jobToUserScore));
        userToJobScore = Math.max(0, Math.min(100, userToJobScore));
        
        // Создаем результат
        EvaluationResult result = new EvaluationResult();
        
        EvaluationResult.FitScore jobToUserFit = new EvaluationResult.FitScore();
        jobToUserFit.setScore(jobToUserScore);
        jobToUserFit.setReasons(List.of("Эвристическая оценка на основе навыков"));
        result.setJobToUserFit(jobToUserFit);
        
        EvaluationResult.UserToJobFit userToJobFit = new EvaluationResult.UserToJobFit();
        userToJobFit.setScore(userToJobScore);
        userToJobFit.setReasons(List.of("Эвристическая оценка на основе навыков"));
        userToJobFit.setMissingSkills(SkillMatcher.findMissingSkills(userProfile.getSkills(), vacancy.getSkills()));
        result.setUserToJobFit(userToJobFit);
        
        // Определяем рекомендацию
        if (jobToUserScore >= 70 && userToJobScore >= 70) {
            result.setSuggestion("Apply");
        } else {
            result.setSuggestion("Skip");
        }
        
        result.setConfidence(0.5); // Низкая уверенность для эвристики
        
        return result;
    }
    
    /**
     * Закрывает WebDriver
     */
    public void close() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                log.warn("Ошибка при закрытии драйвера: {}", e.getMessage());
            }
        }
    }
} 