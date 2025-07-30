package app.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Утилиты для работы с текстом: нормализация, токенизация, очистка
 */
public class TextUtils {
    
    /**
     * Нормализует текст: удаляет лишние пробелы, HTML-теги, приводит к нижнему регистру
     */
    public static String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        return text
                .replaceAll("<[^>]*>", "") // Удаляем HTML-теги
                .replaceAll("&[a-zA-Z]+;", "") // Удаляем HTML-сущности
                .replaceAll("\\s+", " ") // Заменяем множественные пробелы на один
                .trim()
                .toLowerCase();
    }
    
    /**
     * Токенизирует текст по запятым, точкам с запятой и точкам
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }
        
        String normalized = normalizeText(text);
        return Arrays.stream(normalized.split("[,;.]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
    
    /**
     * Извлекает навыки из текста описания вакансии
     */
    public static List<String> extractSkills(String description) {
        if (description == null || description.trim().isEmpty()) {
            return List.of();
        }
        
        // Ищем навыки в описании (обычно выделены тегами или упоминаются в списках)
        String normalized = normalizeText(description);
        
        // Простая эвристика: ищем слова, которые могут быть навыками
        List<String> potentialSkills = Arrays.stream(normalized.split("\\s+"))
                .filter(word -> word.length() > 2 && word.length() < 20)
                .filter(word -> !word.matches(".*\\d.*")) // Исключаем слова с цифрами
                .filter(word -> !isCommonWord(word)) // Исключаем обычные слова
                .distinct()
                .collect(Collectors.toList());
        
        return potentialSkills;
    }
    
    /**
     * Проверяет, является ли слово обычным (не навыком)
     */
    private static boolean isCommonWord(String word) {
        List<String> commonWords = List.of(
                "и", "в", "во", "не", "что", "он", "на", "я", "с", "со", "как", "а", "то", "все", "она",
                "так", "его", "но", "да", "ты", "к", "у", "же", "вы", "за", "бы", "по", "только", "ее",
                "мне", "было", "вот", "от", "меня", "еще", "нет", "о", "из", "ему", "теперь", "когда",
                "даже", "ну", "вдруг", "ли", "если", "уже", "или", "ни", "быть", "был", "него", "до",
                "вас", "нибудь", "опять", "уж", "вам", "ведь", "там", "потом", "себя", "ничего", "ей",
                "может", "они", "тут", "где", "есть", "надо", "ней", "для", "мы", "тебя", "их", "чем",
                "была", "сам", "чтоб", "без", "будто", "чего", "раз", "тоже", "себе", "под", "будет",
                "ж", "тогда", "кто", "этот", "того", "потому", "этого", "какой", "совсем", "ним", "здесь",
                "этом", "один", "почти", "мой", "тем", "чтобы", "нее", "сейчас", "были", "куда", "зачем",
                "всех", "никогда", "можно", "при", "наконец", "два", "об", "другой", "хоть", "после",
                "над", "больше", "тот", "через", "эти", "нас", "про", "всего", "них", "какая", "много",
                "разве", "три", "эту", "моя", "впрочем", "хорошо", "свою", "этой", "перед", "иногда",
                "лучше", "чуть", "том", "нельзя", "такой", "им", "более", "всегда", "конечно", "всю",
                "между", "это", "всё", "работа", "компания", "проект", "задача", "разработка", "опыт",
                "знание", "умение", "навык", "технология", "инструмент", "метод", "подход", "решение"
        );
        
        return commonWords.contains(word.toLowerCase());
    }
    
    /**
     * Ограничивает длину текста до указанного количества символов
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        // Обрезаем до последнего пробела, чтобы не разрывать слова
        String truncated = text.substring(0, maxLength);
        int lastSpace = truncated.lastIndexOf(' ');
        
        if (lastSpace > maxLength * 0.8) { // Если пробел находится в последних 20% текста
            return truncated.substring(0, lastSpace) + "...";
        } else {
            return truncated + "...";
        }
    }
    
    /**
     * Удаляет HTML-теги из текста
     */
    public static String removeHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replaceAll("<[^>]*>", "");
    }
} 