# HH Matcher - Быстрый старт

## Установка и запуск

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

### 4. Соберите проект
```bash
./gradlew build
```

### 5. Запустите приложение
```bash
java -jar build/libs/hh-matcher-standalone.jar --vacancyId=123456 --userProfile=./examples/user.sample.json
```

## Пример использования

1. Найдите ID вакансии на hh.ru (из URL: https://hh.ru/vacancy/123456 → ID = 123456)

2. Создайте профиль пользователя в JSON формате (см. `examples/user.sample.json`)

3. Запустите оценку:
```bash
java -jar hh-matcher.jar --vacancyId=123456 --userProfile=./user.json
```

4. Получите результат в консоли и в файле `out/result_123456_YYYYMMDD_HHMMSS.json`

## Параметры

- `--vacancyId=ID` - ID вакансии на hh.ru
- `--userProfile=PATH` - путь к JSON файлу с профилем
- `--model=MODEL` - модель Ollama (по умолчанию: llama3:8b)
- `--ollama=URL` - URL Ollama сервера (по умолчанию: http://localhost:11434)

## Результат

Приложение выведет:
- Информацию о вакансии
- Оценку соответствия (0-100 баллов)
- Рекомендацию: Apply или Skip
- Уровень уверенности (0.0-1.0)
- Недостающие навыки

Результат также сохраняется в JSON файл в папке `out/`. 