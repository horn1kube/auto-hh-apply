package app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.utils.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Клиент для работы с Ollama API
 */
@Service
public class OllamaClient {
    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);
    
    private final OllamaAPI ollamaAPI;
    private final ObjectMapper objectMapper;
    private static final String DEFAULT_MODEL = "llama3:8b";
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    
    public OllamaClient() {
        this.ollamaAPI = new OllamaAPI(DEFAULT_BASE_URL);
        this.ollamaAPI.setRequestTimeoutSeconds(30);
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Проверяет доступность Ollama
     */
    public boolean isAvailable() {
        try {
            List<Model> models = ollamaAPI.listModels();
            log.info("Доступные модели Ollama: {}", models);
            return !models.isEmpty();
        } catch (Exception e) {
            log.debug("Ollama недоступна: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Получает список доступных моделей
     */
    public List<Model> getAvailableModels() {
        try {
            return ollamaAPI.listModels();
        } catch (Exception e) {
            log.error("Ошибка получения списка моделей: {}", e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Генерирует ответ на промпт
     */
    public String generate(String prompt) throws Exception {
        log.info("Отправляем запрос к Ollama с моделью: {}", DEFAULT_MODEL);
        
        try {
            // Создаем системное сообщение
            OllamaChatMessage systemMessage = new OllamaChatMessage();
            systemMessage.setRole(OllamaChatMessageRole.SYSTEM);
            systemMessage.setContent("Ты - эксперт по оценке соответствия вакансий и кандидатов. Отвечай строго в JSON формате.");
            
            // Создаем пользовательское сообщение
            OllamaChatMessage userMessage = new OllamaChatMessage();
            userMessage.setRole(OllamaChatMessageRole.USER);
            userMessage.setContent(prompt);
            
            // Создаем запрос
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(DEFAULT_MODEL);
            request.setMessages(new ArrayList<>(List.of(systemMessage, userMessage)));
            request.setStream(false);
            request.setOptions(new OptionsBuilder().setTemperature(0.1f).setTopP(0.9f).build().getOptionsMap());
            
            // Отправляем запрос
            OllamaChatResult result = ollamaAPI.chat(request);
            
            if (result != null && result.getResponseModel().getMessage() != null) {
                String response = result.getResponseModel().getMessage().getContent();
                log.info("Получен ответ от Ollama, длина: {} символов", response.length());
                
                // Извлекаем JSON из ответа
                return extractJsonFromResponse(response);
            } else {
                throw new RuntimeException("Пустой ответ от Ollama");
            }
            
        } catch (Exception e) {
            log.error("Ошибка при вызове Ollama: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка вызова Ollama: " + e.getMessage(), e);
        }
    }
    
    /**
     * Извлекает JSON из ответа LLM
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }
        
        // Ищем JSON в фигурных скобках
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);
        
        if (matcher.find()) {
            String json = matcher.group();
            log.debug("Извлечен JSON из ответа: {}", json);
            return json;
        }
        
        // Если JSON не найден, возвращаем весь ответ
        log.warn("JSON не найден в ответе, возвращаем весь текст");
        return response;
    }
    
    /**
     * Генерирует ответ с указанной моделью
     */
    public String generateWithModel(String prompt, String model) throws Exception {
        log.info("Отправляем запрос к Ollama с моделью: {}", model);
        
        try {
            // Создаем системное сообщение
            OllamaChatMessage systemMessage = new OllamaChatMessage();
            systemMessage.setRole(OllamaChatMessageRole.SYSTEM);
            systemMessage.setContent("Ты - эксперт по оценке соответствия вакансий и кандидатов. Отвечай строго в JSON формате.");
            
            // Создаем пользовательское сообщение
            OllamaChatMessage userMessage = new OllamaChatMessage();
            userMessage.setRole(OllamaChatMessageRole.USER);
            userMessage.setContent(prompt);
            
            // Создаем запрос
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(model);
            request.setMessages(List.of(systemMessage, userMessage));
            request.setStream(false);
            request.setOptions(new OptionsBuilder().setTemperature(0.1f).setTopP(0.9f).build().getOptionsMap());
            
            // Отправляем запрос
            OllamaChatResult result = ollamaAPI.chat(request);
            
            if (result != null && result.getResponseModel().getMessage() != null) {
                String response = result.getResponseModel().getMessage().getContent();
                log.info("Получен ответ от Ollama, длина: {} символов", response.length());
                
                // Извлекаем JSON из ответа
                return extractJsonFromResponse(response);
            } else {
                throw new RuntimeException("Пустой ответ от Ollama");
            }
            
        } catch (Exception e) {
            log.error("Ошибка при вызове Ollama с моделью {}: {}", model, e.getMessage(), e);
            throw new RuntimeException("Ошибка вызова Ollama: " + e.getMessage(), e);
        }
    }
} 