package app.service;

import app.model.UserProfile;
import app.service.OllamaClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис для парсинга PDF резюме
 */
@Service
public class ResumeParserService {
    private static final Logger log = LoggerFactory.getLogger(ResumeParserService.class);
    
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;
    
    public ResumeParserService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Парсит PDF резюме и создает UserProfile
     */
    public UserProfile parseResume(MultipartFile file) throws IOException {
        log.info("Начинаем парсинг резюме: {}", file.getOriginalFilename());
        
        String text = extractTextFromPdf(file);
        log.info("Извлечен текст из PDF, длина: {} символов", text.length());
        
        // Сначала пробуем парсинг с помощью Ollama
        if (ollamaClient.isAvailable()) {
            try {
                return createUserProfileWithOllama(text);
            } catch (Exception e) {
                log.warn("Ошибка парсинга с Ollama, используем эвристический метод: {}", e.getMessage());
            }
        }
        
        // Fallback на эвристический метод
        return createUserProfileFromText(text);
    }
    
    /**
     * Извлекает текст из PDF файла
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Создает UserProfile из текста резюме
     */
    private UserProfile createUserProfileFromText(String text) {
        UserProfile profile = new UserProfile();
        
        // Извлекаем имя
        String fullName = extractFullName(text);
        profile.setFullName(fullName);
        
        // Извлекаем опыт работы
        int experienceYears = extractExperienceYears(text);
        profile.setExperienceYears(experienceYears);
        
        // Извлекаем навыки
        List<String> skills = extractSkills(text);
        profile.setSkills(skills);
        
        // Извлекаем стек технологий
        List<String> stack = extractStack(text);
        profile.setStack(stack);
        
        // Извлекаем предпочитаемые локации
        List<String> locations = extractPreferredLocations(text);
        profile.setPreferredLocations(locations);
        
        // Извлекаем формат работы
        String workFormat = extractWorkFormat(text);
        profile.setPreferredWorkFormat(workFormat);
        
        // Извлекаем зарплатные ожидания
        UserProfile.SalaryExpectations salary = extractSalaryExpectations(text);
        profile.setSalaryExpectations(salary);
        
        // Извлекаем языки
        Map<String, String> languages = extractLanguages(text);
        profile.setLanguages(languages);
        
        // Создаем краткое описание
        String summary = createSummary(fullName, experienceYears, skills);
        profile.setSummary(summary);
        
        // Дополнительная информация
        Map<String, String> extras = createExtras(text);
        profile.setExtras(extras);
        
        log.info("Создан профиль для: {} (опыт: {} лет, навыков: {})", 
                fullName, experienceYears, skills.size());
        
        return profile;
    }
    
    /**
     * Извлекает полное имя
     */
    private String extractFullName(String text) {
        // Ищем имя в начале документа
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.length() > 5 && line.length() < 50 && 
                line.matches("^[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+.*")) {
                return line.split("\\s+")[0] + " " + line.split("\\s+")[1];
            }
        }
        return "Неизвестно";
    }
    
    /**
     * Извлекает опыт работы в годах
     */
    private int extractExperienceYears(String text) {
        // Ищем упоминания опыта работы
        Pattern pattern = Pattern.compile("(\\d+)\\s*(лет|год|года)\\s*(опыт|стаж)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        // Альтернативный поиск
        pattern = Pattern.compile("опыт\\s*(?:работы)?\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        
        // Поиск по датам работы
        pattern = Pattern.compile("(\\d{4})\\s*[-–—]\\s*(\\d{4}|по\\s*настоящее\\s*время|present)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(text);
        
        int totalYears = 0;
        while (matcher.find()) {
            try {
                int startYear = Integer.parseInt(matcher.group(1));
                int endYear;
                if (matcher.group(2).toLowerCase().contains("настоящее") || 
                    matcher.group(2).toLowerCase().contains("present")) {
                    endYear = java.time.Year.now().getValue();
                } else {
                    endYear = Integer.parseInt(matcher.group(2));
                }
                totalYears += (endYear - startYear);
            } catch (NumberFormatException e) {
                // Игнорируем некорректные даты
            }
        }
        
        if (totalYears > 0) {
            return totalYears;
        }
        
        // Поиск по ключевым словам
        String lowerText = text.toLowerCase();
        if (lowerText.contains("senior") || lowerText.contains("старший")) {
            return 5;
        } else if (lowerText.contains("middle") || lowerText.contains("средний")) {
            return 3;
        } else if (lowerText.contains("junior") || lowerText.contains("младший")) {
            return 1;
        }
        
        return 2; // По умолчанию
    }
    
    /**
     * Извлекает навыки
     */
    private List<String> extractSkills(String text) {
        List<String> skills = new ArrayList<>();
        
        // Список популярных навыков для поиска
        String[] commonSkills = {
            "Java", "Spring", "Spring Boot", "Hibernate", "Maven", "Gradle",
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Docker", "Kubernetes",
            "Git", "Jenkins", "JUnit", "Mockito", "REST API", "GraphQL",
            "JavaScript", "TypeScript", "React", "Angular", "Vue.js",
            "Python", "C++", "C#", ".NET", "PHP", "Ruby", "Go",
            "AWS", "Azure", "Google Cloud", "Linux", "Windows", "MacOS"
        };
        
        String lowerText = text.toLowerCase();
        
        for (String skill : commonSkills) {
            if (lowerText.contains(skill.toLowerCase())) {
                skills.add(skill);
            }
        }
        
        // Ищем навыки в специальных секциях
        Pattern pattern = Pattern.compile("навыки?[\\s\\S]*?([А-Яа-я\\w\\s,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String skillsText = matcher.group(1);
            String[] foundSkills = skillsText.split("[,;]");
            for (String skill : foundSkills) {
                skill = skill.trim();
                if (skill.length() > 2 && skill.length() < 30) {
                    skills.add(skill);
                }
            }
        }
        
        return skills.stream().distinct().toList();
    }
    
    /**
     * Создает UserProfile с помощью Ollama
     */
    private UserProfile createUserProfileWithOllama(String text) throws Exception {
        log.info("Парсим резюме с помощью Ollama");
        
        // Ограничиваем длину текста для LLM
        String truncatedText = text.length() > 8000 ? text.substring(0, 8000) + "..." : text;
        
        String prompt = String.format("""
            Ты - эксперт по анализу резюме. Проанализируй текст резюме и извлеки структурированную информацию.
            
            ТЕКСТ РЕЗЮМЕ:
            %s
            
            Извлеки следующую информацию и верни строго в JSON формате:
            {
              "fullName": "Полное имя",
              "experienceYears": число лет опыта работы,
              "skills": ["навык1", "навык2", "навык3"],
              "stack": ["технология1", "технология2"],
              "preferredLocations": ["город1", "город2"],
              "preferredWorkFormat": "remote|hybrid|office",
              "salaryExpectations": {
                "min": минимальная зарплата в рублях,
                "max": максимальная зарплата в рублях,
                "current": текущая зарплата в рублях
              },
              "languages": {
                "Русский": "Родной",
                "Английский": "B1|B2|C1"
              },
              "summary": "Краткое описание на основе резюме"
            }
            
            Важные правила:
            - experienceYears должно быть числом (например, 5, а не "5 лет")
            - salaryExpectations должны быть числами в рублях
            - preferredWorkFormat: "remote", "hybrid" или "office"
            - Если информация не найдена, используй разумные значения по умолчанию
            """, truncatedText);
        
        String response = ollamaClient.generate(prompt);
        log.info("Получен ответ от Ollama для парсинга резюме");
        
        return parseUserProfileFromJson(response);
    }
    
    /**
     * Парсит UserProfile из JSON ответа Ollama
     */
    private UserProfile parseUserProfileFromJson(String jsonResponse) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            
            UserProfile profile = new UserProfile();
            
            // Парсим основные поля
            if (jsonNode.has("fullName")) {
                profile.setFullName(jsonNode.get("fullName").asText());
            }
            
            if (jsonNode.has("experienceYears")) {
                profile.setExperienceYears(jsonNode.get("experienceYears").asInt());
            }
            
            if (jsonNode.has("summary")) {
                profile.setSummary(jsonNode.get("summary").asText());
            }
            
            // Парсим навыки
            if (jsonNode.has("skills") && jsonNode.get("skills").isArray()) {
                List<String> skills = new ArrayList<>();
                for (JsonNode skillNode : jsonNode.get("skills")) {
                    skills.add(skillNode.asText());
                }
                profile.setSkills(skills);
            }
            
            // Парсим стек
            if (jsonNode.has("stack") && jsonNode.get("stack").isArray()) {
                List<String> stack = new ArrayList<>();
                for (JsonNode techNode : jsonNode.get("stack")) {
                    stack.add(techNode.asText());
                }
                profile.setStack(stack);
            }
            
            // Парсим локации
            if (jsonNode.has("preferredLocations") && jsonNode.get("preferredLocations").isArray()) {
                List<String> locations = new ArrayList<>();
                for (JsonNode locationNode : jsonNode.get("preferredLocations")) {
                    locations.add(locationNode.asText());
                }
                profile.setPreferredLocations(locations);
            }
            
            // Парсим формат работы
            if (jsonNode.has("preferredWorkFormat")) {
                profile.setPreferredWorkFormat(jsonNode.get("preferredWorkFormat").asText());
            }
            
            // Парсим зарплатные ожидания
            if (jsonNode.has("salaryExpectations")) {
                JsonNode salaryNode = jsonNode.get("salaryExpectations");
                int min = salaryNode.has("min") ? salaryNode.get("min").asInt() : 80000;
                int max = salaryNode.has("max") ? salaryNode.get("max").asInt() : 200000;
                int current = salaryNode.has("current") ? salaryNode.get("current").asInt() : 120000;
                profile.setSalaryExpectations(new UserProfile.SalaryExpectations(min, max, current));
            }
            
            // Парсим языки
            if (jsonNode.has("languages")) {
                JsonNode languagesNode = jsonNode.get("languages");
                Map<String, String> languages = new java.util.HashMap<>();
                languagesNode.fieldNames().forEachRemaining(lang -> {
                    languages.put(lang, languagesNode.get(lang).asText());
                });
                profile.setLanguages(languages);
            }
            
            log.info("Успешно создан профиль с помощью Ollama: {} (опыт: {} лет)", 
                    profile.getFullName(), profile.getExperienceYears());
            
            return profile;
            
        } catch (Exception e) {
            log.error("Ошибка парсинга JSON от Ollama: {}", e.getMessage());
            throw new RuntimeException("Ошибка парсинга резюме с помощью Ollama", e);
        }
    }
    
    /**
     * Извлекает стек технологий
     */
    private List<String> extractStack(String text) {
        List<String> stack = new ArrayList<>();
        
        // Ищем секцию "Технологии" или "Стек"
        Pattern pattern = Pattern.compile("(технологии|стек|инструменты)[\\s\\S]*?([А-Яа-я\\w\\s,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String stackText = matcher.group(2);
            String[] technologies = stackText.split("[,;]");
            for (String tech : technologies) {
                tech = tech.trim();
                if (tech.length() > 2 && tech.length() < 50) {
                    stack.add(tech);
                }
            }
        }
        
        // Если стек не найден, используем навыки
        if (stack.isEmpty()) {
            return extractSkills(text);
        }
        
        return stack;
    }
    
    /**
     * Извлекает предпочитаемые локации
     */
    private List<String> extractPreferredLocations(String text) {
        List<String> locations = new ArrayList<>();
        
        // Популярные города
        String[] commonCities = {
            "Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань",
            "Нижний Новгород", "Челябинск", "Самара", "Уфа", "Ростов-на-Дону"
        };
        
        String lowerText = text.toLowerCase();
        
        for (String city : commonCities) {
            if (lowerText.contains(city.toLowerCase())) {
                locations.add(city);
            }
        }
        
        // Ищем секцию "Локация" или "Город"
        Pattern pattern = Pattern.compile("(локация|город|место)[\\s\\S]*?([А-Яа-я\\s,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            String locationText = matcher.group(2);
            String[] foundLocations = locationText.split("[,;]");
            for (String location : foundLocations) {
                location = location.trim();
                if (location.length() > 2 && location.length() < 30) {
                    locations.add(location);
                }
            }
        }
        
        // Если локации не найдены, добавляем по умолчанию
        if (locations.isEmpty()) {
            locations.add("Москва");
            locations.add("Санкт-Петербург");
        }
        
        return locations.stream().distinct().toList();
    }
    
    /**
     * Извлекает предпочитаемый формат работы
     */
    private String extractWorkFormat(String text) {
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("удаленн") || lowerText.contains("remote")) {
            return "remote";
        } else if (lowerText.contains("гибрид") || lowerText.contains("hybrid")) {
            return "hybrid";
        } else if (lowerText.contains("офис") || lowerText.contains("office")) {
            return "office";
        }
        
        return "hybrid"; // По умолчанию
    }
    
    /**
     * Извлекает зарплатные ожидания
     */
    private UserProfile.SalaryExpectations extractSalaryExpectations(String text) {
        // Ищем зарплатные ожидания
        Pattern pattern = Pattern.compile("(\\d+)\\s*(?:тыс|k|руб|₽)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        
        List<Integer> salaries = new ArrayList<>();
        while (matcher.find()) {
            int salary = Integer.parseInt(matcher.group(1));
            if (salary > 1000) { // Если указано в тысячах
                salaries.add(salary * 1000);
            } else {
                salaries.add(salary);
            }
        }
        
        if (salaries.size() >= 2) {
            int min = salaries.stream().mapToInt(Integer::intValue).min().orElse(80000);
            int max = salaries.stream().mapToInt(Integer::intValue).max().orElse(200000);
            return new UserProfile.SalaryExpectations(min, max, (min + max) / 2);
        } else if (salaries.size() == 1) {
            int salary = salaries.get(0);
            return new UserProfile.SalaryExpectations(salary, salary * 2, salary);
        }
        
        // По умолчанию
        return new UserProfile.SalaryExpectations(80000, 200000, 120000);
    }
    
    /**
     * Извлекает языки
     */
    private Map<String, String> extractLanguages(String text) {
        Map<String, String> languages = new java.util.HashMap<>();
        
        String lowerText = text.toLowerCase();
        
        if (lowerText.contains("русский") || lowerText.contains("родной")) {
            languages.put("Русский", "Родной");
        }
        
        if (lowerText.contains("английский") || lowerText.contains("english")) {
            if (lowerText.contains("b2") || lowerText.contains("c1")) {
                languages.put("Английский", "B2-C1");
            } else if (lowerText.contains("b1")) {
                languages.put("Английский", "B1");
            } else {
                languages.put("Английский", "B1");
            }
        }
        
        return languages;
    }
    
    /**
     * Создает краткое описание
     */
    private String createSummary(String fullName, int experienceYears, List<String> skills) {
        return String.format("%s - разработчик с %d годами опыта. Навыки: %s", 
                fullName, experienceYears, 
                skills.stream().limit(5).reduce("", (a, b) -> a + ", " + b).replaceFirst("^, ", ""));
    }
    
    /**
     * Создает дополнительную информацию
     */
    private Map<String, String> createExtras(String text) {
        Map<String, String> extras = new java.util.HashMap<>();
        
        // Извлекаем образование
        Pattern educationPattern = Pattern.compile("образование[\\s\\S]*?([А-Яа-я\\s]+)", Pattern.CASE_INSENSITIVE);
        Matcher educationMatcher = educationPattern.matcher(text);
        if (educationMatcher.find()) {
            extras.put("education", educationMatcher.group(1).trim());
        }
        
        // Извлекаем интересы
        Pattern interestsPattern = Pattern.compile("интересы[\\s\\S]*?([А-Яа-я\\s,]+)", Pattern.CASE_INSENSITIVE);
        Matcher interestsMatcher = interestsPattern.matcher(text);
        if (interestsMatcher.find()) {
            extras.put("interests", interestsMatcher.group(1).trim());
        }
        
        return extras;
    }
} 