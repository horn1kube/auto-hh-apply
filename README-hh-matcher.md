# HH Matcher - Автоматическая оценка вакансий

Java-приложение для автоматической оценки соответствия вакансий и пользователей с использованием локальной LLM (Ollama).

## Возможности

- 🔍 Автоматический парсинг вакансий с hh.ru
- 🤖 Оценка соответствия с помощью локальной LLM
- 📊 Детальный анализ навыков и требований
- 💡 Рекомендации "Apply" или "Skip"
- 📁 Сохранение результатов в JSON

## Требования

- Java 17+
- Chrome/Chromium браузер (для Selenium)
- Ollama с установленной моделью (например, llama3:8b)

## Установка

### 1. Установка Ollama

Скачайте и установите Ollama с [официального сайта](https://ollama.ai/).

### 2. Установка модели

```bash
# Установка модели llama3:8b
ollama pull llama3:8b

# Или другой модели
ollama pull mistral:7b
```

### 3. Запуск Ollama

```bash
# Запуск Ollama сервера
ollama serve
```

### 4. Сборка проекта

```bash
# Сборка JAR файла
./gradlew build

# Или с пропуском тестов
./gradlew build -x test
```

## Использование

### Базовое использование

```bash
java -jar build/libs/hh-matcher-*.jar --vacancyId=123456 --userProfile=./examples/user.sample.json
```

### Параметры командной строки

| Параметр | Описание | По умолчанию |
|----------|----------|--------------|
| `--vacancyId=ID` | ID вакансии на hh.ru | Обязательный |
| `--userProfile=PATH` | Путь к JSON файлу с профилем | Обязательный |
| `--model=MODEL` | Модель Ollama | llama3:8b |
| `--ollama=URL` | URL Ollama сервера | http://localhost:11434 |

### Примеры

```bash
# Оценка вакансии с пользовательской моделью
java -jar hh-matcher.jar --vacancyId=123456 --userProfile=./user.json --model=mistral:7b

# Использование другого Ollama сервера
java -jar hh-matcher.jar --vacancyId=123456 --userProfile=./user.json --ollama=http://192.168.1.100:11434
```

## Формат профиля пользователя

Создайте JSON файл с профилем пользователя:

```json
{
  "fullName": "Иван Петров",
  "summary": "Java разработчик с 3 годами опыта",
  "skills": ["Java", "Spring Boot", "Hibernate", "PostgreSQL"],
  "experienceYears": 3,
  "stack": ["Java 8-17", "Spring Framework", "Hibernate"],
  "preferredLocations": ["Москва", "Санкт-Петербург", "remote"],
  "preferredWorkFormat": "hybrid",
  "salaryExpectations": {
    "min": 150000,
    "max": 250000,
    "current": 180000
  },
  "languages": {
    "Русский": "Родной",
    "Английский": "B2"
  },
  "extras": {
    "education": "Высшее техническое",
    "certifications": "Oracle Certified Professional"
  }
}
```

## Результат

Приложение выводит детальный анализ и сохраняет результат в файл `out/result_XXXXXX_YYYYMMDD_HHMMSS.json`:

```json
{
  "jobToUserFit": {
    "score": 78,
    "reasons": ["Совпадает стек технологий", "Локация подходит"]
  },
  "userToJobFit": {
    "score": 72,
    "reasons": ["Опыт 3+ года покрывает требования"],
    "missingSkills": ["Kubernetes"]
  },
  "suggestion": "Apply",
  "confidence": 0.82
}
```

## Конфигурация

Создайте файл `src/main/resources/application.properties` для настройки:

```properties
# Конфигурация Ollama
ollama.baseUrl=http://localhost:11434
ollama.model=llama3:8b
ollama.temperature=0.0
ollama.mirostat=0
ollama.topP=0.9

# Таймауты
request.timeoutSec=20
```

## Тестирование

```bash
# Запуск всех тестов
./gradlew test

# Запуск конкретного теста
./gradlew test --tests SkillMatcherTest
```

## Структура проекта

```
src/main/java/com/example/hhmatcher/
├── config/          # Конфигурация
├── model/           # Модели данных
├── scraping/        # Скрапинг hh.ru
├── eval/            # Оценка с LLM
├── util/            # Утилиты
└── cli/             # CLI интерфейс
```

## Логирование

Логи сохраняются в:
- Консоль (INFO уровень)
- `logs/hh-matcher.log` (с ротацией по дням)

## Устранение неполадок

### Ollama недоступен
```
Ошибка: Ollama сервер недоступен
Решение: Убедитесь, что Ollama запущен: ollama serve
```

### Модель не найдена
```
Ошибка: Модель llama3:8b не найдена
Решение: Установите модель: ollama pull llama3:8b
```

### Chrome не найден
```
Ошибка: WebDriver не может найти Chrome
Решение: Установите Chrome/Chromium или укажите путь в переменной CHROME_PATH
```

### Ошибка парсинга
```
Ошибка: Не удалось распарсить вакансию
Решение: Проверьте ID вакансии и доступность страницы
```

## Разработка

### Добавление новых моделей

1. Установите модель: `ollama pull model-name`
2. Укажите в конфигурации: `ollama.model=model-name`

### Расширение парсинга

Добавьте новые селекторы в `HhVacancyScraper`:

```java
private static final String[] NEW_FIELD_SELECTORS = {
    "[data-qa=\"new-field\"]",
    ".new-field-class"
};
```

### Кастомизация промптов

Измените `PromptBuilder` для адаптации под ваши нужды.

## Лицензия

MIT License

## Поддержка

При возникновении проблем:
1. Проверьте логи в `logs/hh-matcher.log`
2. Убедитесь, что Ollama запущен и модель установлена
3. Проверьте доступность Chrome/Chromium
4. Создайте issue с описанием проблемы 