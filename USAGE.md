# HH Matcher - Инструкция по использованию

## Быстрый старт

### 1. Установите Ollama
```bash
# Скачайте с https://ollama.ai/
# Или используйте curl (Linux/macOS):
curl -fsSL https://ollama.ai/install.sh | sh
```

### 2. Установите модель
```bash
ollama pull llama3:8b
```

### 3. Запустите Ollama
```bash
ollama serve
```

### 4. Запустите приложение
```bash
java -jar build/libs/AutoVacancyReply-1.0.0-standalone.jar --vacancyId=123456 --userProfile=./examples/user.sample.json
```

## Примеры использования

### Базовое использование
```bash
java -jar AutoVacancyReply-1.0.0-standalone.jar --vacancyId=123456 --userProfile=./user.json
```

### С пользовательской моделью
```bash
java -jar AutoVacancyReply-1.0.0-standalone.jar --vacancyId=123456 --userProfile=./user.json --model=mistral:7b
```

### С другим Ollama сервером
```bash
java -jar AutoVacancyReply-1.0.0-standalone.jar --vacancyId=123456 --userProfile=./user.json --ollama=http://192.168.1.100:11434
```

## Параметры

| Параметр | Описание | Обязательный | По умолчанию |
|----------|----------|--------------|--------------|
| `--vacancyId=ID` | ID вакансии на hh.ru | Да | - |
| `--userProfile=PATH` | Путь к JSON файлу с профилем | Да | - |
| `--model=MODEL` | Модель Ollama | Нет | llama3:8b |
| `--ollama=URL` | URL Ollama сервера | Нет | http://localhost:11434 |

## Формат профиля пользователя

Создайте JSON файл с профилем пользователя (см. `examples/user.sample.json`):

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

Приложение выведет:
- Информацию о вакансии
- Оценку соответствия (0-100 баллов)
- Рекомендацию: Apply или Skip
- Уровень уверенности (0.0-1.0)
- Недостающие навыки

Результат также сохраняется в JSON файл в папке `out/`.

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
Решение: Установите Chrome/Chromium
```

### Ошибка парсинга
```
Ошибка: Не удалось распарсить вакансию
Решение: Проверьте ID вакансии и доступность страницы
``` 